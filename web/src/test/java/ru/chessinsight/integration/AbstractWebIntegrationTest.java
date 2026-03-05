package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import ru.chessinsight.infrastructure.web.WebApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebApplication.class, WebContainerTestConfig.class}
)
@ContextConfiguration(initializers = WebTestDatabaseInitializer.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.main.log-startup-info=false",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR"
})
public abstract class AbstractWebIntegrationTest {

    private static final int STOCKFISH_PORT = 5555;
    private static final String STOCKFISH_PROVIDER = resolveStockfishProvider();

    static final GenericContainer<?> STOCKFISH = new GenericContainer<>(stockfishImage(STOCKFISH_PROVIDER))
            .withExposedPorts(STOCKFISH_PORT);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (STOCKFISH.isRunning()) {
                STOCKFISH.stop();
            }
        }));
    }

    @DynamicPropertySource
    static void registerStockfishProperties(DynamicPropertyRegistry registry) {
        ensureStockfishStarted();
        String host = STOCKFISH.getHost();
        Integer port = STOCKFISH.getMappedPort(STOCKFISH_PORT);
        registry.add("engine.stockfish.mode", () -> "tcp");
        registry.add("engine.stockfish.host", () -> host);
        registry.add("engine.stockfish.port", () -> port);
        registry.add("engine.stockfish.defaultDepth", () -> 8);
    }

    private static synchronized void ensureStockfishStarted() {
        if (!STOCKFISH.isRunning()) {
            STOCKFISH.start();
        }
    }

    private static ImageFromDockerfile stockfishImage(String provider) {
        String fixture = switch (provider) {
            case "mock" -> "stockfish-mock";
            case "real" -> "stockfish-real";
            default -> throw new IllegalStateException(
                    "Unsupported test.stockfish.provider='" + provider + "'. Expected: mock|real");
        };

        Path fixtureDir = resolveFixtureDir(fixture);
        return new ImageFromDockerfile(
                "chessinsight-" + fixture + "-" + UUID.randomUUID(),
                false
        ).withFileFromPath(".", fixtureDir);
    }

    private static Path resolveFixtureDir(String fixture) {
        Path[] candidates = new Path[] {
                Path.of("test-fixtures", fixture),
                Path.of("..", "test-fixtures", fixture)
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Stockfish fixture directory not found: " + fixture);
    }

    private static String resolveStockfishProvider() {
        String value = System.getProperty(
                "test.stockfish.provider",
                System.getenv().getOrDefault("TEST_STOCKFISH_PROVIDER", "mock")
        );
        return value.toLowerCase(Locale.ROOT).trim();
    }
}

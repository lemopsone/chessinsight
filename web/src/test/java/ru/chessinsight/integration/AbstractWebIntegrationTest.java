package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import ru.chessinsight.infrastructure.web.WebApplication;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebApplication.class, WebContainerTestConfig.class}
)
@ContextConfiguration(initializers = WebTestDatabaseInitializer.class)
@TestPropertySource(properties = {
        "engine.stockfish.mode=mock",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.main.log-startup-info=false",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR"
})
public abstract class AbstractWebIntegrationTest {
}

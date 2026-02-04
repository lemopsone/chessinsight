package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.chessinsight.infrastructure.web.WebApplication;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebApplication.class, WebContainerTestConfig.class}
)
@TestPropertySource(properties = {
        "engine.stockfish.mode=mock",
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class AbstractWebIntegrationTest {
}
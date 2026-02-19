package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import ru.chessinsight.ChessInsight;

@ContextConfiguration(initializers = IntegrationTestDatabaseInitializer.class)
@SpringBootTest(classes = {ContainerTestConfig.class, ChessInsight.class})
@TestPropertySource(properties = {
        "spring.main.log-startup-info=false",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR"
})
public abstract class AbstractIntegrationTest {}

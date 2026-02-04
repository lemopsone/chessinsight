package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import ru.chessinsight.ChessInsight;
import org.junit.jupiter.api.Tag;

@Tag("integration")
@SpringBootTest(classes = {ContainerTestConfig.class, ChessInsight.class})
public abstract class AbstractIntegrationTest {}
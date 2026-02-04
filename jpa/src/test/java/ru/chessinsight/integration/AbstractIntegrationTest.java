package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import ru.chessinsight.ChessInsight;

@SpringBootTest(classes = {ContainerTestConfig.class, ChessInsight.class})
public abstract class AbstractIntegrationTest {}
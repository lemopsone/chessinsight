package ru.chessinsight.integration;

import org.springframework.boot.test.context.SpringBootTest;
import ru.chessinsight.ChessInsight;
import ru.chessinsight.testutil.Tag;

@Tag("integration")
@SpringBootTest(classes = {ContainerTestConfig.class, ChessInsight.class})
public abstract class AbstractIntegrationTest {}
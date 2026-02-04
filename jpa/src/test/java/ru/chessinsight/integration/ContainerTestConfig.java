package ru.chessinsight.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EngineInfo;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;

import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class ContainerTestConfig {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("schema.sql");
    }

    @Bean
    @Primary
    ChessEngine chessEngine() {
        return new ChessEngine() {
            @Override
            public EngineInfo info() {
                return new EngineInfo("mock", "test", "1", Map.of());
            }

            @Override
            public EnginePositionAnalysis analyzePosition(EngineAnalysisRequest request) {
                return new EnginePositionAnalysis(
                        request.positionFEN(),
                        request.depth(),
                        null,
                        "e2e4",
                        "e4",
                        List.of("e2e4", "e7e5"),
                        List.of("e4", "e5"),
                        10,
                        null,
                        List.of()
                );
            }

            @Override
            public EngineMoveAnalysis analyzeMove(EngineMoveRequest request) {
                return new EngineMoveAnalysis(
                        request.positionFEN(),
                        request.playedMoveUci(),
                        null,
                        0,
                        null,
                        "e2e4",
                        "e4",
                        0,
                        List.of("e2e4"),
                        List.of("e4")
                );
            }
        };
    }
}

package ru.chessinsight.infrastructure.engine;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;
import ru.chessinsight.application.game.analysis.engine.dto.EngineAnalysisRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EngineInfo;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.analysis.engine.dto.EnginePositionAnalysis;
import ru.chessinsight.application.game.analysis.engine.exception.EngineException;

@Service
@Profile("test")
public class MockEngine implements ChessEngine {
    @Override
    public EngineInfo info() throws EngineException {
        return null;
    }

    @Override
    public EnginePositionAnalysis analyzePosition(EngineAnalysisRequest request) throws EngineException {
        return null;
    }

    @Override
    public EngineMoveAnalysis analyzeMove(EngineMoveRequest request) throws EngineException {
        return null;
    }
}

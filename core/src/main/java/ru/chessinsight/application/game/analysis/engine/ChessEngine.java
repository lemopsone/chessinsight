package ru.chessinsight.application.game.analysis.engine;

import ru.chessinsight.application.game.analysis.engine.dto.*;
import ru.chessinsight.application.game.analysis.engine.exception.EngineException;

/**
 * ChessEngine focuses solely on retrieving data from the chess engine.
 * No persistence or training-scenario creation responsibilities here.
 */
public interface ChessEngine {
    /** Basic metadata such as name/author/version/options if available. */
    EngineInfo info() throws EngineException;

    /** Analyze a position and return the best line (and optionally multi-PV candidates). */
    EnginePositionAnalysis analyzePosition(EngineAnalysisRequest request) throws EngineException;

    /** Analyze a concrete played move compared to the engine's best line. */
    EngineMoveAnalysis analyzeMove(EngineMoveRequest request) throws EngineException;
}

package ru.chessinsight.application.game.analysis.engine.dto;

import java.util.Map;

public record EngineInfo(
        String name,
        String author,
        String version,
        Map<String, String> options
) {}

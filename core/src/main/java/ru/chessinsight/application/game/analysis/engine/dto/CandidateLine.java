package ru.chessinsight.application.game.analysis.engine.dto;

import java.util.List;

public record CandidateLine(
        int index,
        List<String> pvUci,
        List<String> pvSan,
        Integer scoreCp,
        Integer scoreMate
) {}

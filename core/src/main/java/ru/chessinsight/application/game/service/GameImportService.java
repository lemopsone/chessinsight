package ru.chessinsight.application.game.service;

import java.util.Optional;
import java.util.UUID;

public interface GameImportService {

    UUID importFromPgn(UUID ownerId, String pgn, String startFen, String resultTag);

    UUID importFromMoves(UUID ownerId, String format, String movesText, String startFen, String resultTag);
}

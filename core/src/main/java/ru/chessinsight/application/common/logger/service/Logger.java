package ru.chessinsight.application.common.logger.service;

public interface Logger {
    void info(String message);
    void warning(String message);
    void error(String message);
}

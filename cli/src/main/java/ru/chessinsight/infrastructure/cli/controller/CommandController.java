package ru.chessinsight.infrastructure.cli.controller;

public interface CommandController {
    String name();
    String description();
    void handle(String[] args);
}

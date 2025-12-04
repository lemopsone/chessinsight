package ru.chessinsight.infrastructure.cli.exception;

public class CliAuthRequiredException extends CliException {
    public CliAuthRequiredException() {
        super("You must be signed in to run this command.");
    }
}

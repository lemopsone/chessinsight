package ru.chessinsight.infrastructure.cli.exception;

public class CliUsageException extends CliException {
    private final String usage;

    public CliUsageException(String message, String usage) {
        super(message);
        this.usage = usage;
    }

    public String getUsage() { return usage; }
}

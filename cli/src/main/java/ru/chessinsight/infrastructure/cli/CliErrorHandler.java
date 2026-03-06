package ru.chessinsight.infrastructure.cli;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;

import jakarta.validation.ConstraintViolationException;

import ru.chessinsight.infrastructure.cli.exception.CliAuthRequiredException;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;
import ru.chessinsight.infrastructure.cli.exception.CliValidationException;

public final class CliErrorHandler {
    private static final boolean DEBUG = Boolean.parseBoolean(
            System.getenv().getOrDefault("CLI_DEBUG", "false")
    );

    private CliErrorHandler() {}

    public static void handle(Throwable t) {
        if (handleCliException(t)) {
            return;
        }
        if (handleGeneralException(t)) {
            return;
        }
        println("Unexpected error: " + rootMessage(t));
        debug(t);
    }

    private static boolean handleCliException(Throwable t) {
        if (t instanceof CliUsageException e) {
            println("Usage error: " + e.getMessage());
            if (e.getUsage() != null && !e.getUsage().isBlank()) {
                println("Usage: " + e.getUsage());
            }
            return true;
        }
        if (t instanceof CliAuthRequiredException) {
            println("Auth required: " + t.getMessage());
            println("run `auth signin <login> <password>`.");
            return true;
        }
        if (t instanceof CliValidationException) {
            println("Validation failed: " + t.getMessage());
            return true;
        }
        if (t instanceof CliNotFoundException) {
            println("Not found: " + t.getMessage());
            return true;
        }
        return false;
    }

    private static boolean handleGeneralException(Throwable t) {
        if (t instanceof ConstraintViolationException) {
            println("Validation error: " + t.getMessage());
            return true;
        }
        if (t instanceof DataAccessException) {
            println("Database error: " + rootMessage(t));
            debug(t);
            return true;
        }
        if (t instanceof AccessDeniedException) {
            println("Access denied: " + t.getMessage());
            return true;
        }
        if (t instanceof IllegalArgumentException) {
            println("Bad input: " + t.getMessage());
            return true;
        }
        if (t instanceof IllegalStateException) {
            println("Operation failed: " + t.getMessage());
            return true;
        }
        return false;
    }

    private static void println(String s) {
        System.out.println("[ERROR] " + s);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }

    private static void debug(Throwable t) {
        if (DEBUG) {
            System.out.println("--- DEBUG (CLI_DEBUG=true) ---");
            t.printStackTrace(System.out);
        }
    }
}

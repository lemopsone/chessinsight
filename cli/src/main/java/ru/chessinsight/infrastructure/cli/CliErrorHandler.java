package ru.chessinsight.infrastructure.cli;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;

import jakarta.validation.ConstraintViolationException;

import ru.chessinsight.infrastructure.cli.exception.*;

public final class CliErrorHandler {
    private static final boolean DEBUG = Boolean.parseBoolean(
            System.getenv().getOrDefault("CLI_DEBUG", "false")
    );

    private CliErrorHandler() {}

    public static void handle(Throwable t) {
        if (t instanceof CliUsageException e) {
            println("Usage error: " + e.getMessage());
            if (e.getUsage() != null && !e.getUsage().isBlank()) {
                println("Usage: " + e.getUsage());
            }
            return;
        }
        if (t instanceof CliAuthRequiredException) {
            println("Auth required: " + t.getMessage());
            println("run `auth signin <login> <password>`.");
            return;
        }
        if (t instanceof CliValidationException) {
            println("Validation failed: " + t.getMessage());
            return;
        }
        if (t instanceof CliNotFoundException) {
            println("Not found: " + t.getMessage());
            return;
        }
        if (t instanceof ConstraintViolationException) {
            println("Validation error: " + t.getMessage());
            return;
        }
        if (t instanceof DataAccessException) {
            println("Database error: " + rootMessage(t));
            debug(t);
            return;
        }
        if (t instanceof AccessDeniedException) {
            println("Access denied: " + t.getMessage());
            return;
        }
        if (t instanceof IllegalArgumentException) {
            println("Bad input: " + t.getMessage());
            return;
        }
        if (t instanceof IllegalStateException) {
            println("Operation failed: " + t.getMessage());
            return;
        }

        println("Unexpected error: " + rootMessage(t));
        debug(t);
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

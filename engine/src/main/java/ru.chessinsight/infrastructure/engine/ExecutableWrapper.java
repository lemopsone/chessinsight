package ru.chessinsight.infrastructure.engine;

import ru.chessinsight.application.game.analysis.engine.exception.EngineException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Thin wrapper around an external UCI-compatible engine process.
 * Provides safe command sending and utilities to read output until
 * synchronization tokens like "uciok" / "readyok".
 */
public abstract class ExecutableWrapper implements Closeable {
    protected Process process;
    protected BufferedReader reader;
    protected BufferedWriter writer;

    protected ExecutableWrapper(String path) throws IOException {
        startProcess(path);
    }

    protected ExecutableWrapper(BufferedReader reader, BufferedWriter writer) {
        this.reader = reader;
        this.writer = writer;
        this.process = null;
    }


    protected void startProcess(String path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(path);
        pb.redirectErrorStream(true);
        this.process = pb.start();
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    public void sendCommand(String command) throws EngineException {
        try {
            writer.write(command);
            writer.write('\n');
            writer.flush();
        } catch (Exception e) {
            throw new EngineException("Failed to send command '%s': %s".formatted(command, e.getMessage()));
        }
    }

    /**
     * Read a single line from the engine (blocking).
     */
    protected String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * Read lines until one of the given tokens exactly matches the whole line.
     * Returns the entire transcript.
     */
    protected String readUntil(Duration timeout, String... stopTokens) throws IOException {
        Instant deadline = Instant.now().plus(timeout);
        StringBuilder sb = new StringBuilder();
        while (Instant.now().isBefore(deadline)) {
            String line = reader.readLine();
            if (line == null) break;
            sb.append(line).append('\n');
            for (String t : stopTokens) {
                if (line.equals(t)) {
                    return sb.toString();
                }
            }
        }
        throw new IOException("Timeout waiting for tokens " + Arrays.toString(stopTokens));
    }

    @Override
    public void close() throws IOException {
        try {
            writer.write("quit\n");
            writer.flush();
        } catch (Exception ignored) {}
        try {
            process.destroy();
        } finally {
            reader.close();
            writer.close();
        }
    }

    abstract public String getOutput() throws IOException;
}

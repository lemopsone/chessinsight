package ru.chessinsight.infrastructure.security.service;

public interface PasswordHasher {
    String hash(String raw);
    boolean verify(String raw, String hash);
}

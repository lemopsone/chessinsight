package ru.chessinsight.application.auth.dto;

public record SignUpDTO(
        String login,
        String email,
        String password
){}

package ru.chessinsight.application.user.service;

import ru.chessinsight.domain.user.model.User;

import java.util.UUID;

public interface UserProfileService {
    User updateProfile(UUID userId, String email, String password);
}

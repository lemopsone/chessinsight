package ru.chessinsight.application.auth.service;

import ru.chessinsight.application.auth.dto.*;
import ru.chessinsight.application.auth.exception.*;
import ru.chessinsight.domain.user.model.User;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface AuthService {
    UserTokenDTO signUp(SignUpDTO dto) throws UserExistsException;
    UserTokenDTO signIn(SignInDTO dto) throws WrongCredentialsException, AuthException;
    void requestAccountRecovery(RecoveryRequestDTO dto);
    void confirmAccountRecovery(RecoveryConfirmDTO dto) throws AuthException;
    UserTokenDTO refresh(String refreshToken) throws AuthException;
    void signOut(@Nullable String refreshToken);

    Optional<UUID> getCurrentUserId();

    Optional<User> getCurrentUser();
}

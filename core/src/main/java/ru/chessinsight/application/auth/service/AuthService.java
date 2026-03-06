package ru.chessinsight.application.auth.service;

import org.springframework.lang.Nullable;
import ru.chessinsight.application.auth.dto.RecoveryConfirmDTO;
import ru.chessinsight.application.auth.dto.RecoveryRequestDTO;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.exception.AuthException;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.exception.WrongCredentialsException;
import ru.chessinsight.domain.user.model.User;

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

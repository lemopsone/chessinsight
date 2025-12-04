package ru.chessinsight.infrastructure.web.mapper;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.auth.dto.AuthType;

@Component
public class AuthApiMapper {

    public ru.chessinsight.application.auth.dto.SignUpDTO toAppSignUp(
            ru.chessinsight.infrastructure.web.dto.SignUpDTO dto
    ) {
        if (dto == null) {
            return null;
        }
        return new ru.chessinsight.application.auth.dto.SignUpDTO(
                dto.getLogin(),
                dto.getEmail(),
                dto.getPassword()
        );
    }

    public ru.chessinsight.application.auth.dto.SignInDTO toAppSignIn(
            ru.chessinsight.infrastructure.web.dto.SignInDTO dto
    ) {
        if (dto == null) {
            return null;
        }
        AuthType authType = dto.getAuthType() != null
                ? AuthType.valueOf(dto.getAuthType().name())
                : AuthType.JWT;

        return new ru.chessinsight.application.auth.dto.SignInDTO(
                dto.getLoginOrEmail(),
                dto.getPasswordOrToken(),
                authType
        );
    }

    public ru.chessinsight.infrastructure.web.dto.UserTokenDTO toApiUserToken(
            ru.chessinsight.application.auth.dto.UserTokenDTO token
    ) {
        if (token == null) {
            return null;
        }
        ru.chessinsight.infrastructure.web.dto.UserTokenDTO dto =
                new ru.chessinsight.infrastructure.web.dto.UserTokenDTO();
        dto.setAccessToken(token.accessToken());
        dto.setRefreshToken(token.refreshToken());
        dto.setTokenType(token.tokenType());
        return dto;
    }
}

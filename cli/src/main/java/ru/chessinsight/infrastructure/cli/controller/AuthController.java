package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.application.auth.dto.AuthType;
import ru.chessinsight.application.auth.dto.SignInDTO;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.dto.UserTokenDTO;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

@Component
public class AuthController implements CommandController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    static String accessToken;

    @Override public String name() { return "auth"; }
    @Override public String description() { return "User authorization"; }

    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            throw new CliUsageException("Missing subcommand.",
                    "auth signup <login> <email> <password> | auth signin <login> <password> | signout | me");
        }
        switch (args[0]) {
            case "signup" -> {
                if (args.length < 4) {
                    throw new CliUsageException("Not enough arguments.",
                            "auth signup <login> <email> <password>");
                }
                UserTokenDTO tok = authService.signUp(new SignUpDTO(args[1], args[2], args[3]));
                System.out.println("Signed up. Access=" + tok.accessToken());
            }
            case "signin" -> {
                if (args.length < 3) {
                    throw new CliUsageException("Not enough arguments.",
                            "auth signin <login> <password>");
                }
                UserTokenDTO tok = authService.signIn(new SignInDTO(args[1], args[2], AuthType.JWT));
                System.out.println("Signed in. Access=" + tok.accessToken());
                accessToken = tok.accessToken();
            }
            case "signout" -> {
                authService.signOut(accessToken);
                accessToken = null;
            }
            case "me" -> {
                var user = authService.getCurrentUser();
                if (user.isEmpty()) {
                    System.out.println("You're not logged in");
                } else {
                    System.out.printf("You're currently logged as %s (roles=%s)%n", user.get().getLogin(), user.get().getRoles());
                }
            }
            default -> throw new CliUsageException("Unknown subcommand.",
                    "auth signup <login> <email> <password> | auth signin <login> <password> | signout | me");
        }
    }
}

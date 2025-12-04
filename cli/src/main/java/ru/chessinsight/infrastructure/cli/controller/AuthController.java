package ru.chessinsight.infrastructure.cli.controller;

import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.cli.api.SessionContext;
import ru.chessinsight.infrastructure.cli.api.WebApiClient;
import ru.chessinsight.infrastructure.cli.api.dto.ApiUser;
import ru.chessinsight.infrastructure.cli.api.dto.ApiUserToken;
import ru.chessinsight.infrastructure.cli.exception.CliUsageException;

@Component
public class AuthController implements CommandController {

    private final WebApiClient api;
    private final SessionContext session;

    public AuthController(WebApiClient api, SessionContext session) {
        this.api = api;
        this.session = session;
    }

    @Override public String name() { return "auth"; }
    @Override public String description() { return "User authorization"; }

    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            throw new CliUsageException("Missing subcommand.",
                    """
                            auth signup <login> <email> <password>
                            auth signin <login> <password>
                            auth signout
                            auth me""");
        }
        switch (args[0]) {
            case "signup" -> handleSignup(args);
            case "signin" -> handleSignin(args);
            case "signout" -> handleSignout();
            case "me"     -> handleMe();
            default -> throw new CliUsageException("Unknown subcommand.",
                    """
                            auth signup <login> <email> <password>
                            auth signin <login> <password>
                            auth signout
                            auth me""");
        }
    }

    private void handleSignup(String[] args) {
        if (args.length < 4) {
            throw new CliUsageException("Not enough arguments.",
                    "auth signup <login> <email> <password>");
        }
        ApiUserToken tok = api.signUp(args[1], args[2], args[3]);
        System.out.println("Signed up. tokenType=" + tok.tokenType());
    }

    private void handleSignin(String[] args) {
        if (args.length < 3) {
            throw new CliUsageException("Not enough arguments.",
                    "auth signin <login> <password>");
        }
        ApiUserToken tok = api.signIn(args[1], args[2]);
        System.out.println("Signed in. tokenType=" + tok.tokenType());
    }

    private void handleSignout() {
        api.signOut();
        System.out.println("Signed out.");
    }

    private void handleMe() {
        if (!session.isAuthenticated()) {
            System.out.println("You're not logged in");
            return;
        }
        ApiUser user = api.getCurrentUser();
        System.out.printf("Logged as %s (id=%s, roles=%s)%n",
                user.getLogin(), user.getId(),
                user.getRoles() != null ? String.join(",", user.getRoles()) : "-");
    }
}

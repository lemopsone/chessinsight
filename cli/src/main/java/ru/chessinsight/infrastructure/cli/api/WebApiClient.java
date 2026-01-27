package ru.chessinsight.infrastructure.cli.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import ru.chessinsight.infrastructure.cli.api.dto.*;
import ru.chessinsight.infrastructure.cli.exception.CliAuthRequiredException;
import ru.chessinsight.infrastructure.cli.exception.CliException;
import ru.chessinsight.infrastructure.cli.exception.CliNotFoundException;
import ru.chessinsight.infrastructure.cli.exception.CliValidationException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class WebApiClient {

    private final RestClient restClient;
    private final SessionContext session;

    public WebApiClient(
            SessionContext session,
            @Value("${chess.api.base-url:http://localhost:8080/api/v1}") String baseUrl
    ) {
        this.session = session;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ApiUserToken signUp(String login, String email, String password) {
        Map<String, Object> body = Map.of(
                "login", login,
                "email", email,
                "password", password
        );
        try {
            return restClient.post()
                    .uri("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApiUserToken.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiUserToken signIn(String login, String password) {
        Map<String, Object> body = Map.of(
                "loginOrEmail", login,
                "passwordOrToken", password,
                "authType", "JWT"
        );
        try {
            ApiUserToken token = restClient.post()
                    .uri("/auth/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApiUserToken.class);

            session.setAccessToken(token.accessToken());
            session.setRefreshToken(token.refreshToken());

            ApiUser me = getCurrentUser();
            session.setUserId(me.getId());
            session.setLogin(me.getLogin());
            session.setRoles(Set.copyOf(me.getRoles()));

            return token;
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public void signOut() {
        if (!session.isAuthenticated()) return;
        try {
            String refreshToken = session.getRefreshToken();
            if (refreshToken != null && !refreshToken.isBlank()) {
                restClient.method(HttpMethod.DELETE)
                        .uri("/auth/sessions")
                        .headers(this::applyAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("refreshToken", refreshToken))
                        .retrieve()
                        .toBodilessEntity();
            }
        } catch (HttpStatusCodeException e) {
            // игнорируем
        } finally {
            session.clear();
        }
    }

    public ApiUser getCurrentUser() {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        try {
            return restClient.get()
                    .uri("/users/me")
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ApiUser.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    // ========= GAMES =========

    public ApiPageResponse<ApiGameSummary> listMyGames(Integer page, Integer size) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/me/games")
                            .queryParam("page", page != null ? page : 0)
                            .queryParam("size", size != null ? size : 20)
                            .build())
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiPageResponse<ApiGameSummary>>() {});
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiGameSummary importGameFromPgn(String pgn, String startFen, String resultTag) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("pgn", pgn);
        if (startFen != null) body.put("startFen", startFen);
        if (resultTag != null) body.put("resultTag", resultTag);

        try {
            return restClient.post()
                    .uri("/games")
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApiGameSummary.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiGameAnalysis analyzeGame(UUID gameId) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        try {
            return restClient.post()
                    .uri("/games/{id}/analysis", gameId)
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ApiGameAnalysis.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiMoveAnalysis analyzeMove(String positionFEN, String moveSan, String moveUci, Integer moveNum) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        var body = new java.util.HashMap<String, Object>();
        if (moveNum != null) body.put("moveNum", moveNum);
        body.put("positionFEN", positionFEN);
        body.put("moveSAN", moveSan);
        body.put("moveUCI", moveUci);

        try {
            return restClient.post()
                    .uri("/move-evaluations")
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApiMoveAnalysis.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    // ========= TRAINING =========

    public ApiPageResponse<ApiTrainingScenario> listTrainingScenarios(Boolean completed, Integer page, Integer size) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/users/me/training-scenarios")
                                .queryParam("page", page != null ? page : 0)
                                .queryParam("size", size != null ? size : 20);
                        if (completed != null) {
                            b.queryParam("completed", completed);
                        }
                        return b.build();
                    })
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiPageResponse<ApiTrainingScenario>>() {});
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiTrainingScenario getTrainingScenario(UUID scenarioId) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        try {
            return restClient.get()
                    .uri("/training-scenarios/{id}", scenarioId)
                    .headers(this::applyAuth)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ApiTrainingScenario.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    public ApiTrainingMoveResponse submitTrainingMove(UUID scenarioId, Integer cursor, String moveUci, boolean demo) {
        if (!session.isAuthenticated()) {
            throw new CliAuthRequiredException();
        }
        var body = new java.util.HashMap<String, Object>();
        if (cursor != null) body.put("cursor", cursor);
        body.put("moveUCI", moveUci);
        body.put("isDemo", demo);

        try {
            return restClient.post()
                    .uri("/training-scenarios/{id}/moves", scenarioId)
                    .headers(this::applyAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApiTrainingMoveResponse.class);
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        }
    }

    private void applyAuth(HttpHeaders headers) {
        if (session.getAccessToken() != null) {
            headers.setBearerAuth(session.getAccessToken());
        }
    }

    private RuntimeException mapHttpException(HttpStatusCodeException e) {
        int code = e.getStatusCode().value();

        if (code == 400) {
            return new CliValidationException("Bad request: " + e.getResponseBodyAsString());
        }
        if (code == 401) {
            session.clear();
            return new CliAuthRequiredException();
        }
        if (code == 403) {
            return new org.springframework.security.access.AccessDeniedException("Forbidden");
        }
        if (code == 404) {
            return new CliNotFoundException("Not found");
        }
        return new CliException("Remote API error: " + code + " " + e.getStatusText());
    }
}

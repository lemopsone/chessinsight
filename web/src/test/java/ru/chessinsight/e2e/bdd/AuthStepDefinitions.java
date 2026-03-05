package ru.chessinsight.e2e.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.chessinsight.integration.InMemoryAuthCodeDeliveryService;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthStepDefinitions {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InMemoryAuthCodeDeliveryService authCodeDeliveryService;

    private String login;
    private String email;
    private String initialPassword;
    private String currentPassword;
    private String rotatedPassword;
    private String accessToken;
    private ResponseEntity<String> lastResponse;

    @After("@auth")
    void cleanupTechnicalUser() {
        if (login == null) {
            return;
        }
        userRepository.findOneByLogin(login).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
        authCodeDeliveryService.clearForEmail(email);
    }

    @Given("a technical user is prepared for authentication checks")
    public void provisionTechnicalUser() {
        login = "tech_user_" + UUID.randomUUID().toString().replace("-", "");
        email = login + "@example.com";

        String fromEnv = System.getenv("E2E_TECH_USER_PASSWORD");
        initialPassword = (fromEnv != null && !fromEnv.isBlank())
                ? fromEnv
                : generatePassword("Initial");

        currentPassword = initialPassword;
        rotatedPassword = null;
        accessToken = null;
        lastResponse = null;

        Map<String, Object> signUpBody = new LinkedHashMap<>();
        signUpBody.put("login", login);
        signUpBody.put("email", email);
        signUpBody.put("password", initialPassword);

        ResponseEntity<String> signUpResponse = restTemplate.postForEntity(url("/users"), signUpBody, String.class);
        assertEquals(HttpStatus.CREATED, signUpResponse.getStatusCode(), signUpResponse.getBody());
    }

    @When("the user signs in with valid password as first factor")
    public void signInWithPasswordFirstFactor() {
        signInFirstFactor(currentPassword);
    }

    @Then("the response requires an email one-time code")
    public void verifySecondFactorRequired() {
        assertEquals(HttpStatus.UNAUTHORIZED, lastResponse.getStatusCode());
        assertTrue(extractDetail(lastResponse).contains("Second factor required"));
    }

    @When("the user confirms sign in with the email one-time code")
    public void confirmSecondFactor() {
        signInSecondFactor(requireCode(authCodeDeliveryService.latestOtpCode(email), "OTP"));
    }

    @Then("JWT tokens are issued for the user")
    public void verifyJwtTokensIssued() {
        assertEquals(HttpStatus.OK, lastResponse.getStatusCode(), lastResponse.getBody());
        String issuedAccessToken = extract(lastResponse, "accessToken");
        String refreshToken = extract(lastResponse, "refreshToken");
        assertFalse(issuedAccessToken.isBlank());
        assertFalse(refreshToken.isBlank());
        accessToken = issuedAccessToken;
    }

    @When("the user enters a wrong password {int} times")
    public void enterWrongPasswordTimes(int attempts) {
        for (int i = 1; i <= attempts; i++) {
            signInFirstFactor("wrong_password_" + i);
        }
    }

    @Then("the account is blocked")
    public void verifyAccountBlocked() {
        assertEquals(HttpStatus.LOCKED, lastResponse.getStatusCode(), lastResponse.getBody());
    }

    @And("sign in with the correct password is rejected because account is blocked")
    public void verifyCorrectPasswordStillBlocked() {
        signInFirstFactor(currentPassword);
        assertEquals(HttpStatus.LOCKED, lastResponse.getStatusCode(), lastResponse.getBody());
    }

    @And("the account is blocked because of wrong password attempts")
    public void blockAccountByWrongPasswords() {
        enterWrongPasswordTimes(3);
        verifyAccountBlocked();
    }

    @When("recovery is requested for the locked account")
    public void requestRecoveryForLockedAccount() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginOrEmail", login);

        lastResponse = restTemplate.postForEntity(url("/auth/recovery/request"), body, String.class);
        assertEquals(HttpStatus.ACCEPTED, lastResponse.getStatusCode(), lastResponse.getBody());
    }

    @And("recovery is confirmed with a new password")
    public void confirmRecoveryWithNewPassword() {
        rotatedPassword = generatePassword("Recovered");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginOrEmail", login);
        body.put("recoveryCode", requireCode(authCodeDeliveryService.latestRecoveryCode(email), "recovery"));
        body.put("newPassword", rotatedPassword);

        lastResponse = restTemplate.postForEntity(url("/auth/recovery/confirm"), body, String.class);
        assertEquals(HttpStatus.NO_CONTENT, lastResponse.getStatusCode(), lastResponse.getBody());
        currentPassword = rotatedPassword;
    }

    @Then("sign in with the old password fails")
    public void oldPasswordFails() {
        signInFirstFactor(initialPassword);
        assertEquals(HttpStatus.UNAUTHORIZED, lastResponse.getStatusCode(), lastResponse.getBody());
    }

    @And("sign in with the rotated password requires second factor")
    public void rotatedPasswordRequiresSecondFactor() {
        signInFirstFactor(currentPassword);
        verifySecondFactorRequired();
    }

    @And("confirming the second factor issues JWT tokens")
    public void confirmingSecondFactorIssuesTokens() {
        signInSecondFactor(requireCode(authCodeDeliveryService.latestOtpCode(email), "OTP"));
        verifyJwtTokensIssued();
    }

    @And("the user is fully authenticated with email second factor")
    public void userFullyAuthenticated() {
        signInWithPasswordFirstFactor();
        verifySecondFactorRequired();
        confirmSecondFactor();
        verifyJwtTokensIssued();
    }

    @When("the user rotates the password in the profile")
    public void rotatePasswordInProfile() {
        rotatedPassword = generatePassword("Rotated");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> patchBody = new LinkedHashMap<>();
        patchBody.put("password", rotatedPassword);

        lastResponse = restTemplate.exchange(
                url("/users/me"),
                HttpMethod.PATCH,
                new HttpEntity<>(patchBody, headers),
                String.class
        );

        assertEquals(HttpStatus.OK, lastResponse.getStatusCode(), lastResponse.getBody());
        currentPassword = rotatedPassword;
    }

    private void signInFirstFactor(String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginOrEmail", login);
        body.put("passwordOrToken", password);
        body.put("authType", "JWT");
        lastResponse = restTemplate.postForEntity(url("/auth/sessions"), body, String.class);
    }

    private void signInSecondFactor(String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginOrEmail", login);
        body.put("passwordOrToken", code);
        body.put("authType", "EMAIL_OTP");
        lastResponse = restTemplate.postForEntity(url("/auth/sessions"), body, String.class);
    }

    private String extract(ResponseEntity<String> response, String field) {
        if (response.getBody() == null || response.getBody().isBlank()) {
            return "";
        }
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path(field).asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractDetail(ResponseEntity<String> response) {
        String detail = extract(response, "detail");
        return detail == null ? "" : detail;
    }

    private String generatePassword(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "") + "Aa1!";
    }

    private String requireCode(String code, String kind) {
        assertTrue(code != null && !code.isBlank(), "No " + kind + " code was delivered");
        return code;
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }
}

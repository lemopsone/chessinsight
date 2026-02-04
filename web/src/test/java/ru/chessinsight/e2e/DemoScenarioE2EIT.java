package ru.chessinsight.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.chessinsight.integration.AbstractWebIntegrationTest;

import java.util.UUID;
import java.util.stream.StreamSupport;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("e2e")
public class DemoScenarioE2EIT extends AbstractWebIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void demoScenario_importsAndDeletesGame() throws Exception {
        String login = "user_" + UUID.randomUUID();
        ObjectNode signUp = objectMapper.createObjectNode();
        signUp.put("login", login);
        signUp.put("email", login + "@example.com");
        signUp.put("password", "pass12345");

        ResponseEntity<String> signUpResp = restTemplate.postForEntity(url("/api/v1/users"), signUp, String.class);
        assertEquals(HttpStatus.CREATED, signUpResp.getStatusCode());
        JsonNode signUpJson = objectMapper.readTree(signUpResp.getBody());
        String accessToken = signUpJson.path("accessToken").asText();
        assertFalse(accessToken.isBlank());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String pgn = "[Event \"Casual\"]\n" +
                "[Site \"Here\"]\n" +
                "[Date \"2024.01.01\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"W\"]\n" +
                "[Black \"B\"]\n" +
                "[Result \"*\"]\n\n" +
                "1. e4 e5 *";

        ObjectNode importReq = objectMapper.createObjectNode();
        importReq.put("pgn", pgn);

        ResponseEntity<String> importResp = restTemplate.exchange(
                url("/api/v1/games"),
                HttpMethod.POST,
                new HttpEntity<>(importReq, headers),
                String.class
        );
        assertEquals(HttpStatus.CREATED, importResp.getStatusCode());
        JsonNode importJson = objectMapper.readTree(importResp.getBody());
        String gameId = importJson.path("id").asText();
        assertFalse(gameId.isBlank());

        ResponseEntity<String> listResp = restTemplate.exchange(
                url("/api/v1/users/me/games"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        JsonNode content = objectMapper.readTree(listResp.getBody()).path("content");
        boolean found = StreamSupport.stream(content.spliterator(), false)
                .anyMatch(node -> gameId.equals(node.path("id").asText()));
        assertTrue(found);

        ResponseEntity<String> getResp = restTemplate.exchange(
                url("/api/v1/games/" + gameId),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertEquals(HttpStatus.OK, getResp.getStatusCode());

        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                url("/api/v1/games/" + gameId),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
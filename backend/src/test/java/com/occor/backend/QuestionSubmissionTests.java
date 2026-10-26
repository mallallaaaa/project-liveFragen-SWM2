package com.occor.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:question-submission-tests")
class QuestionSubmissionTests {
    @LocalServerPort
    private int port;

    @Autowired
    private QuestionRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void savesTrimmedQuestionInH2AndReturnsCreated() throws Exception {
        long count = repository.count();
        var response = post("{\"text\":\"  How does LiveQ work?  \"}");

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"text\":\"How does LiveQ work?\""));
        assertEquals(count + 1, repository.count());
        var saved = repository.findAll().stream()
                .filter(question -> question.getText().equals("How does LiveQ work?")).findFirst().orElseThrow();
        assertNotNull(saved.getId());
        assertNotNull(saved.getSubmittedAt());
        assertTrue(response.body().contains("\"id\":" + saved.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"text\":null}", "{\"text\":\"\"}",
            "{\"text\":\"   \\t\\n\"}"})
    void rejectsEmptyQuestionsWithoutSaving(String body) throws Exception {
        long count = repository.count();
        var response = post(body);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"message\":\"Question cannot be empty\""));
        assertEquals(count, repository.count());
    }

    @Test
    void enforcesDatabaseLengthLimit() throws Exception {
        long count = repository.count();
        var tooLong = post("{\"text\":\"" + "a".repeat(501) + "\"}");
        assertEquals(400, tooLong.statusCode());
        assertTrue(tooLong.body().contains("Question is too long"));
        assertEquals(count, repository.count());
        assertEquals(201, post("{\"text\":\"" + "a".repeat(500) + "\"}").statusCode());
        assertEquals(count + 1, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "{invalid json"})
    void rejectsMalformedRequestsWithoutSaving(String body) throws Exception {
        long count = repository.count();
        assertEquals(400, post(body).statusCode());
        assertEquals(count, repository.count());
    }

    @Test
    void servesFrontendAndAssetsFromTheApiOrigin() throws Exception {
        for (String path : new String[]{"question.html", "question.js", "blue1.png"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + path))
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().length > 0, path);
        }
    }
}

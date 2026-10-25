package com.occor.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:question-list-tests")
class QuestionListTests {
    @LocalServerPort
    private int port;

    @Autowired
    private QuestionRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearTestDatabase() {
        repository.deleteAll();
    }

    private HttpResponse<String> getQuestions() throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void returnsEmptyJsonArrayWhenNoQuestionsExist() throws Exception {
        var response = getQuestions();
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
        assertEquals("[]", response.body());
    }

    @Test
    void returnsSavedQuestionsInSubmissionOrderWithTheirIdsAndTimestamps() throws Exception {
        var first = repository.save(new Question("First question?"));
        var second = repository.save(new Question("Second question?"));

        var response = getQuestions();
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":" + first.getId()));
        assertTrue(response.body().contains("\"id\":" + second.getId()));
        assertTrue(response.body().contains("\"submittedAt\":"));
        assertTrue(response.body().contains("Second question?"));
        assertTrue(response.body().indexOf("First question?") < response.body().indexOf("Second question?"));
        assertEquals(2, repository.count());
    }

    @Test
    void newlySubmittedQuestionAppearsOnNextGet() throws Exception {
        assertEquals("[]", getQuestions().body());
        var created = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"A newly submitted question?\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, created.statusCode());
        assertEquals("[" + created.body() + "]", getQuestions().body());
    }
}

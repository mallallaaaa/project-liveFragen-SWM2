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

    @Autowired
    private EventRepository events;

    private Event event;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearTestDatabase() {
        repository.deleteAll();
        event = events.save(new Event("List test event"));
    }

    private HttpResponse<String> getQuestions() throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions?eventId=" + event.getId()))
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
        var first = repository.save(new Question("First question?", event));
        var second = repository.save(new Question("Second question?", event));

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
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"A newly submitted question?\",\"eventId\":" + event.getId() + "}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, created.statusCode());
        assertEquals("[" + created.body() + "]", getQuestions().body());
    }

    @Test
    void questionsFromOtherEventsAreNeverReturned() throws Exception {
        var otherEvent = events.save(new Event("Other event"));
        for (var target : new Event[]{event, otherEvent}) {
            String text = target == event ? "Visible question" : "Other event question";
            var created = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"" + text + "\",\"eventId\":" + target.getId() + "}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, created.statusCode());
            assertTrue(created.body().contains("\"eventId\":" + target.getId()));
        }
        var response = getQuestions();
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Visible question"));
        assertFalse(response.body().contains("Other event question"));
        var otherResponse = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/api/questions?eventId=" + otherEvent.getId())).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(otherResponse.body().contains("Other event question"));
        assertFalse(otherResponse.body().contains("Visible question"));
    }

    @Test
    void emptyEventDoesNotDisplayQuestionsFromAnotherEvent() throws Exception {
        repository.save(new Question("Other event question", events.save(new Event("Other event"))));
        assertEquals("[]", getQuestions().body());
    }

    @Test
    void missingInvalidAndUnknownEventsCannotReadOrSubmitQuestions() throws Exception {
        for (String query : new String[]{"", "?eventId=0", "?eventId=-1", "?eventId=abc"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                    + "/api/questions" + query)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode(), query);
            assertTrue(response.body().contains("message"));
        }
        var unknown = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/api/questions?eventId=9223372036854775807")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, unknown.statusCode());

        for (String eventField : new String[]{"", ",\"eventId\":0", ",\"eventId\":-1",
                ",\"eventId\":9223372036854775807"}) {
            long count = repository.count();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/questions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"Question\"" + eventField + "}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(eventField.contains("9223372036854775807") ? 404 : 400, response.statusCode());
            assertEquals(count, repository.count());
        }
    }
}

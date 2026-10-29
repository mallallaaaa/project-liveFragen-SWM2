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
        properties = "spring.datasource.url=jdbc:h2:mem:event-creation-tests")
class EventCreationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void savesEventInH2AndReturnsItsIdentity() throws Exception {
        long count = repository.count();
        var response = post("{\"title\":\"  Software Engineering lecture  \"}");
        assertEquals(201, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
        assertEquals(count + 1, repository.count());
        var saved = repository.findAll().stream()
                .filter(event -> event.getTitle().equals("Software Engineering lecture"))
                .findFirst().orElseThrow();
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertTrue(response.body().contains("\"id\":" + saved.getId()));
        assertTrue(response.body().contains("\"title\":\"Software Engineering lecture\""));
        assertTrue(response.body().contains("\"createdAt\":"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\"\"}",
            "{\"title\":\"   \\t\\n\"}", "{\"title\":\"\u2003\"}"})
    void rejectsMissingAndBlankTitlesWithoutSaving(String body) throws Exception {
        long count = repository.count();
        var response = post(body);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"message\":\"Please enter an event title.\""));
        assertEquals(count, repository.count());
    }

    @Test
    void enforcesThe200CharacterLimit() throws Exception {
        long count = repository.count();
        var invalid = post("{\"title\":\"" + "a".repeat(201) + "\"}");
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("200 characters"));
        assertEquals(count, repository.count());
        assertEquals(201, post("{\"title\":\"" + "a".repeat(200) + "\"}").statusCode());
        assertEquals(count + 1, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "{invalid json", "{\"title\":{}}"})
    void rejectsMalformedRequestsWithReadableErrors(String body) throws Exception {
        long count = repository.count();
        var response = post(body);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Please send an event title as JSON."));
        assertEquals(count, repository.count());
    }

    @Test
    void separateEventsHaveDifferentIdsEvenWithTheSameTitle() throws Exception {
        long count = repository.count();
        var first = post("{\"title\":\"Weekly lecture\"}");
        var second = post("{\"title\":\"Weekly lecture\"}");
        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertEquals(count + 2, repository.count());
        var ids = repository.findAll().stream()
                .filter(event -> event.getTitle().equals("Weekly lecture"))
                .map(Event::getId).distinct().toList();
        assertEquals(2, ids.size());
    }

    @Test
    void servesEventPageAndAssetsFromTheApiOrigin() throws Exception {
        for (String path : new String[]{"create-event.html", "create-event.js"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + path))
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().length > 0, path);
        }
    }
}

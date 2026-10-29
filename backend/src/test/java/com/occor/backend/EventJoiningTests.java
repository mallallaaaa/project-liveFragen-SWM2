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
        properties = "spring.datasource.url=jdbc:h2:mem:event-joining-tests")
class EventJoiningTests {
    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void participantCanAccessCreatedEventWithoutRegistrationOrCookies() throws Exception {
        var created = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Open lecture\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, created.statusCode());
        var event = repository.findAll().stream()
                .filter(saved -> saved.getTitle().equals("Open lecture")).findFirst().orElseThrow();
        var page = get("/event.html?eventId=" + event.getId());
        assertEquals(200, page.statusCode());
        var response = get("/api/events/" + event.getId());
        assertEquals(200, response.statusCode());
        assertEquals(created.body(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
    }

    @Test
    void eachLinkReturnsOnlyItsOwnEventAndDoesNotCreateEvents() throws Exception {
        var first = repository.save(new Event("First lecture"));
        var second = repository.save(new Event("Second lecture"));
        long count = repository.count();
        var firstResponse = get("/api/events/" + first.getId());
        var secondResponse = get("/api/events/" + second.getId());
        assertEquals(200, firstResponse.statusCode());
        assertTrue(firstResponse.body().contains("First lecture"));
        assertFalse(firstResponse.body().contains("Second lecture"));
        assertEquals(200, secondResponse.statusCode());
        assertTrue(secondResponse.body().contains("Second lecture"));
        assertEquals(firstResponse.body(), get("/api/events/" + first.getId()).body());
        assertEquals(count, repository.count());
    }

    @Test
    void unknownEventReturns404WithReadableError() throws Exception {
        var response = get("/api/events/9223372036854775807");
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("This event could not be found."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidEventIdReturns400WithReadableError(String id) throws Exception {
        var response = get("/api/events/" + id);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("The event link is invalid."));
    }

    @Test
    void participantJavascriptIsServed() throws Exception {
        var response = get("/event.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("joinEvent"));
    }
}

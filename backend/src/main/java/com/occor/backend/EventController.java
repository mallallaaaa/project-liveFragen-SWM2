package com.occor.backend;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventRepository repository;

    public EventController(EventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The event link is invalid.");
        }
        Event event = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This event could not be found. Please check the link with the presenter."));
        return new EventResponse(event.getId(), event.getTitle(), event.getCreatedAt());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<EventError> handleInvalidId() {
        return ResponseEntity.badRequest().body(new EventError("The event link is invalid."));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@RequestBody CreateEventRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter an event title.");
        }
        String title = request.title().strip();
        if (title.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The event title must be 200 characters or fewer.");
        }

        Event event = repository.save(new Event(title));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EventResponse(event.getId(), event.getTitle(), event.getCreatedAt()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<EventError> handleInvalidEvent(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new EventError(exception.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EventError> handleInvalidBody() {
        return ResponseEntity.badRequest().body(new EventError("Please send an event title as JSON."));
    }

    public record CreateEventRequest(String title) {}
    public record EventResponse(Long id, String title, Instant createdAt) {}
    public record EventError(String message) {}
}


package com.occor.backend;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository repository;
    private final EventRepository events;

    public QuestionController(QuestionRepository repository, EventRepository events) {
        this.repository = repository;
        this.events = events;
    }

    @GetMapping
    public List<QuestionResponse> list(@RequestParam(required = false) Long eventId) {
        Event event = requireEvent(eventId);
        return repository.findByEvent_IdOrderByIdAsc(event.getId()).stream()
                .map(question -> new QuestionResponse(
                        question.getId(), question.getText(), question.getSubmittedAt(), question.getEventId()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> submit(
            @RequestBody CreateQuestionRequest request) {

        if (request.text() == null ||
                request.text().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question cannot be empty"
            );
        }

        String text = request.text().trim();

        if (text.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question is too long"
            );
        }

        Event event = requireEvent(request.eventId());
        Question question = repository.save(
                new Question(text, event)
        );

        QuestionResponse response = new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getSubmittedAt(),
                question.getEventId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private Event requireEvent(Long eventId) {
        if (eventId == null || eventId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid event ID is required.");
        }
        return events.findById(eventId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "This event could not be found. Please check the link with the presenter."));
    }

    public record CreateQuestionRequest(String text, Long eventId) {}

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<QuestionError> handleMalformedQuestion() {
        return ResponseEntity.badRequest().body(new QuestionError("Invalid question request. Please check the event link and question text."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<QuestionError> handleInvalidQuestion(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new QuestionError(exception.getReason()));
    }

    public record QuestionError(String message) {}

    public record QuestionResponse(
            Long id,
            String text,
            Instant submittedAt,
            Long eventId
    ) {}
}


package com.occor.backend;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository repository;

    public QuestionController(QuestionRepository repository) {
        this.repository = repository;
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

        Question question = repository.save(
                new Question(text)
        );

        QuestionResponse response = new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getSubmittedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    public record CreateQuestionRequest(String text) {}

    public record QuestionResponse(
            Long id,
            String text,
            Instant submittedAt
    ) {}
}
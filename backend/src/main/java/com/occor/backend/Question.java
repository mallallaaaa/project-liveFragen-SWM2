package com.occor.backend;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    private Instant submittedAt;

    protected Question() {
    }

    public Question(String text) {
        this.text = text;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}

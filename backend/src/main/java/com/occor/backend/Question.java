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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private Instant submittedAt;

    protected Question() {
    }

    public Question(String text, Event event) {
        this.text = text;
        this.event = event;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Long getEventId() {
        return event.getId();
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}

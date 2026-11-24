
package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "game_attempt_answers",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"attempt_id", "question_id"}
        )
    }
)
public class GameAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private GameAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GameQuestion question;

    @Column(columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean correct = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean evaluated = true;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public GameAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(GameAttempt attempt) {
        this.attempt = attempt;
    }

    public GameQuestion getQuestion() {
        return question;
    }

    public void setQuestion(GameQuestion question) {
        this.question = question;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
}

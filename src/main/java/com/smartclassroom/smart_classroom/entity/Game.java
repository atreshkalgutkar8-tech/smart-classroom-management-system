
package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic game information
    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    // QUIZ, MATCHING or PUZZLE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameType gameType;

    // Mathematics, Kannada, English or EVS
    @Column(nullable = false, length = 30)
    private String subject;

    // Grade 1 to Grade 5
    @Column(nullable = false)
    private Integer grade;

    // PRELOADED or CUSTOM
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSource source;

    // Teacher who created the game.
    // Null for preloaded games.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // AVAILABLE_TO_STUDENTS or HIDDEN_FROM_STUDENTS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameVisibility visibility =
            GameVisibility.AVAILABLE_TO_STUDENTS;

    // Used only for custom puzzle games
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PuzzleTemplate puzzleTemplate;

    // Used only for custom matching games
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MatchingFormat matchingFormat;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public GameSource getSource() {
        return source;
    }

    public void setSource(GameSource source) {
        this.source = source;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public GameVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GameVisibility visibility) {
        this.visibility = visibility;
    }

    public PuzzleTemplate getPuzzleTemplate() {
        return puzzleTemplate;
    }

    public void setPuzzleTemplate(PuzzleTemplate puzzleTemplate) {
        this.puzzleTemplate = puzzleTemplate;
    }

    public MatchingFormat getMatchingFormat() {
        return matchingFormat;
    }

    public void setMatchingFormat(MatchingFormat matchingFormat) {
        this.matchingFormat = matchingFormat;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

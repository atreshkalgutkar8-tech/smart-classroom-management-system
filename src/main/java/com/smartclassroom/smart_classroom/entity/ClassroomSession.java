
package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_sessions")
public class ClassroomSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    // Persist presentation progress separately from the session lifecycle.
    private Integer presentationPosition = 0;

    private LocalDateTime presentationStartedAt;

    private LocalDateTime presentationEndedAt;

    // Teacher who created this classroom session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    public ClassroomSession() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getPresentationPosition() {
        return presentationPosition;
    }

    public void setPresentationPosition(Integer presentationPosition) {
        this.presentationPosition = presentationPosition;
    }

    public LocalDateTime getPresentationStartedAt() {
        return presentationStartedAt;
    }

    public void setPresentationStartedAt(LocalDateTime presentationStartedAt) {
        this.presentationStartedAt = presentationStartedAt;
    }

    public LocalDateTime getPresentationEndedAt() {
        return presentationEndedAt;
    }

    public void setPresentationEndedAt(LocalDateTime presentationEndedAt) {
        this.presentationEndedAt = presentationEndedAt;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }
}

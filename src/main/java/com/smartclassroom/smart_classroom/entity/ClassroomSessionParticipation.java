package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Records a student's participation in one classroom session. */
@Entity
@Table(name = "classroom_session_participations",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_participation_student",
                columnNames = {"classroom_session_id", "student_id"}),
        indexes = {
                @Index(name = "idx_session_participation_session", columnList = "classroom_session_id"),
                @Index(name = "idx_session_participation_student", columnList = "student_id")
        })
public class ClassroomSessionParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_session_id", nullable = false)
    private ClassroomSession classroomSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, updatable = false)
    private LocalDateTime participatedAt;

    protected ClassroomSessionParticipation() { }

    public ClassroomSessionParticipation(ClassroomSession classroomSession, User student) {
        this.classroomSession = classroomSession;
        this.student = student;
    }

    @PrePersist
    void setParticipationTimestamp() {
        if (participatedAt == null) participatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ClassroomSession getClassroomSession() { return classroomSession; }
    public User getStudent() { return student; }
    public LocalDateTime getParticipatedAt() { return participatedAt; }
}

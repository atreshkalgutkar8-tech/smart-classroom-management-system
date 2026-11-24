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

/** One completion record for a student's DIY activity participation. */
@Entity
@Table(name = "diy_activity_participations",
        uniqueConstraints = @UniqueConstraint(name = "uk_diy_activity_student",
                columnNames = {"activity_id", "student_id"}),
        indexes = @Index(name = "idx_diy_participation_student", columnList = "student_id"))
public class DIYActivityParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private DIYActivity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    protected DIYActivityParticipation() { }

    public DIYActivityParticipation(DIYActivity activity, User student) {
        this.activity = activity;
        this.student = student;
    }

    @PrePersist
    void onCreate() { if (completedAt == null) completedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public DIYActivity getActivity() { return activity; }
    public User getStudent() { return student; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

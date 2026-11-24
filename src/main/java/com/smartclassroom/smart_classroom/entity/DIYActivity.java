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
import java.time.LocalDateTime;

@Entity
@Table(name = "diy_activities", indexes = {
        @Index(name = "idx_diy_activity_grade_subject_topic", columnList = "grade, subject, topic")
})
public class DIYActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false, length = 40)
    private String subject;

    @Column(nullable = false, length = 180)
    private String topic;

    @Column(nullable = false, length = 1000)
    private String learningObjective;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requiredMaterials;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_teacher_id", nullable = false)
    private User createdByTeacher;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DIYActivity() { }

    public DIYActivity(String title, Integer grade, String subject, String topic,
            String learningObjective, String requiredMaterials, String instructions, User createdByTeacher) {
        this.title = title;
        this.grade = grade;
        this.subject = subject;
        this.topic = topic;
        this.learningObjective = learningObjective;
        this.requiredMaterials = requiredMaterials;
        this.instructions = instructions;
        this.createdByTeacher = createdByTeacher;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Integer getGrade() { return grade; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getLearningObjective() { return learningObjective; }
    public String getRequiredMaterials() { return requiredMaterials; }
    public String getInstructions() { return instructions; }
    public User getCreatedByTeacher() { return createdByTeacher; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

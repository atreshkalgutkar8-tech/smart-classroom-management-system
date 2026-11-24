package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "classroom_resources", indexes = {
        @Index(name = "idx_classroom_resource_order", columnList = "classroom_session_id, display_order")
})
public class ClassroomResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_session_id", nullable = false)
    private ClassroomSession classroomSession;

    @Column(nullable = false, length = 180)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassroomResourceType resourceType;

    @Column(length = 500)
    private String description;

    @Column(length = 2048)
    private String resourceUrl;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ClassroomSession getClassroomSession() { return classroomSession; }
    public void setClassroomSession(ClassroomSession classroomSession) { this.classroomSession = classroomSession; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ClassroomResourceType getResourceType() { return resourceType; }
    public void setResourceType(ClassroomResourceType resourceType) { this.resourceType = resourceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_messages", indexes = @Index(
        name = "idx_complaint_message_history", columnList = "complaint_id, sent_at"))
public class ComplaintMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "complaint_id", nullable = false)
    private ParentComplaint complaint;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplaintStatus statusAfter;

    @PrePersist
    void onCreate() { if (sentAt == null) sentAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public ParentComplaint getComplaint() { return complaint; }
    public void setComplaint(ParentComplaint complaint) { this.complaint = complaint; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public ComplaintStatus getStatusAfter() { return statusAfter; }
    public void setStatusAfter(ComplaintStatus statusAfter) { this.statusAfter = statusAfter; }
}

package com.jaywant.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientType; // "EMPLOYEE" or "SUBADMIN"

    @Column(nullable = false)
    private Integer recipientId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private String notificationType; // "LEAVE_APPLIED", "LEAVE_APPROVED", "LEAVE_REJECTED"

    private Integer relatedLeaveId;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private Boolean isRead = false;

    private String fcmMessageId;

    // Constructors
    public NotificationLog() {
        this.sentAt = LocalDateTime.now();
    }

    public NotificationLog(String recipientType, Integer recipientId, String title,
                           String body, String notificationType, Integer relatedLeaveId) {
        this();
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.title = title;
        this.body = body;
        this.notificationType = notificationType;
        this.relatedLeaveId = relatedLeaveId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public Integer getRecipientId() { return recipientId; }
    public void setRecipientId(Integer recipientId) { this.recipientId = recipientId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public Integer getRelatedLeaveId() { return relatedLeaveId; }
    public void setRelatedLeaveId(Integer relatedLeaveId) { this.relatedLeaveId = relatedLeaveId; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getFcmMessageId() { return fcmMessageId; }
    public void setFcmMessageId(String fcmMessageId) { this.fcmMessageId = fcmMessageId; }
}
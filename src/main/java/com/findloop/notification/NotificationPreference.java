package com.findloop.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @Column(length = 255)
    @JsonIgnore
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationFrequency matchFrequency = NotificationFrequency.OFF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationFrequency replyFrequency = NotificationFrequency.OFF;

    protected NotificationPreference() {}

    public NotificationPreference(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public NotificationFrequency getMatchFrequency() { return matchFrequency; }
    public NotificationFrequency getReplyFrequency() { return replyFrequency; }
    public void setMatchFrequency(NotificationFrequency frequency) { this.matchFrequency = frequency; }
    public void setReplyFrequency(NotificationFrequency frequency) { this.replyFrequency = frequency; }
}


package com.findloop.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_contact_request", columnNames = "contact_request_id")
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_request_id", nullable = false, unique = true)
    private Long contactRequestId;

    @Column(nullable = false, length = 255)
    private String userOneId;

    @Column(nullable = false, length = 255)
    private String userTwoId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Conversation() {
    }

    public Conversation(
            Long contactRequestId,
            String userOneId,
            String userTwoId) {
        this.contactRequestId = contactRequestId;
        this.userOneId = userOneId;
        this.userTwoId = userTwoId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getContactRequestId() {
        return contactRequestId;
    }

    public String getUserOneId() {
        return userOneId;
    }

    public String getUserTwoId() {
        return userTwoId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean includes(String userId) {
        return userOneId.equals(userId) || userTwoId.equals(userId);
    }
}

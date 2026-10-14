package com.findloop.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop100ByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(String recipientId);
}

package com.findloop.notification;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserNotificationRepository notificationRepository;

    public NotificationService(NotificationPreferenceRepository preferenceRepository,
            UserNotificationRepository notificationRepository) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationPreference preferences(String userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(userId)));
    }

    @Transactional
    public NotificationPreference updatePreferences(String userId, UpdateNotificationPreferencesRequest request) {
        NotificationPreference preference = preferences(userId);
        preference.setMatchFrequency(request.matchFrequency());
        preference.setReplyFrequency(request.replyFrequency());
        return preferenceRepository.save(preference);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMatch(String recipientId, Long reportId, String newReportTitle) {
        saveIfEnabled(recipientId, NotificationKind.MATCH, "A possible match was found",
                "A new report may match your item: " + truncate(newReportTitle, 350), reportId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyReply(String recipientId, Long conversationId, String message) {
        saveIfEnabled(recipientId, NotificationKind.REPLY, "New message",
                truncate(message == null ? "You have a new private message." : message, 500), conversationId);
    }

    private void saveIfEnabled(String recipientId, NotificationKind kind, String title, String message, Long refId) {
        if (recipientId == null || recipientId.isBlank()) return;
        NotificationPreference preference = preferenceRepository.findById(recipientId).orElse(null);
        if (preference == null) return; // Notifications are opt-in.
        NotificationFrequency frequency = kind == NotificationKind.MATCH
                ? preference.getMatchFrequency() : preference.getReplyFrequency();
        if (frequency != null && frequency != NotificationFrequency.OFF) {
            notificationRepository.save(new UserNotification(recipientId, kind, title, message, refId));
        }
    }

    @Transactional(readOnly = true)
    public List<UserNotification> unread(String userId) {
        NotificationPreference preference = preferenceRepository.findById(userId).orElse(null);
        if (preference == null) return List.of();
        LocalDateTime dailyCutoff = LocalDateTime.now().minusDays(1);
        return notificationRepository.findTop100ByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId).stream()
                .filter(notification -> {
                    NotificationFrequency frequency = notification.getKind() == NotificationKind.MATCH
                            ? preference.getMatchFrequency() : preference.getReplyFrequency();
                    return frequency == NotificationFrequency.IMMEDIATELY
                            || (frequency == NotificationFrequency.DAILY
                                    && !notification.getCreatedAt().isAfter(dailyCutoff));
                }).toList();
    }

    @Transactional
    public void markRead(Long notificationId, String userId) {
        UserNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This notification belongs to another user.");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }
}

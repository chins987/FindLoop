package com.findloop.notification;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferencesRequest(
        @NotNull NotificationFrequency matchFrequency,
        @NotNull NotificationFrequency replyFrequency) {}

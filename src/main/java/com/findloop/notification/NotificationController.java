package com.findloop.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Read notifications and manage notification preferences.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences")
    public NotificationPreference preferences(@AuthenticationPrincipal Jwt jwt) {
        return service.preferences(jwt.getSubject());
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    public NotificationPreference updatePreferences(@Valid @RequestBody UpdateNotificationPreferencesRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.updatePreferences(jwt.getSubject(), request);
    }

    @GetMapping
    @Operation(summary = "List unread notifications")
    public List<UserNotification> unread(@AuthenticationPrincipal Jwt jwt) {
        return service.unread(jwt.getSubject());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.markRead(id, jwt.getSubject());
    }
}

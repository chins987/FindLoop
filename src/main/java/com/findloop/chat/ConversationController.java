
package com.findloop.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Open conversations and exchange private messages after a contact request.")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List conversations")
    public List<Conversation> list(
            @AuthenticationPrincipal Jwt jwt) {
        return service.listConversations(jwt.getSubject());
    }

    @PostMapping("/from-request/{requestId}")
    @Operation(summary = "Open a conversation from a request")
    public Conversation open(
            @PathVariable Long requestId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.openConversation(requestId, jwt.getSubject());
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "List conversation messages")
    public List<ChatMessage> messages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getMessages(conversationId, jwt.getSubject());
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a conversation message")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage send(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        return service.sendMessage(
                conversationId, jwt.getSubject(), body.get("content"));
    }
}

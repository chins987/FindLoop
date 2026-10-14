
package com.findloop.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Conversation> list(
            @AuthenticationPrincipal Jwt jwt) {
        return service.listConversations(jwt.getSubject());
    }

    @PostMapping("/from-request/{requestId}")
    public Conversation open(
            @PathVariable Long requestId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.openConversation(requestId, jwt.getSubject());
    }

    @GetMapping("/{conversationId}/messages")
    public List<ChatMessage> messages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getMessages(conversationId, jwt.getSubject());
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage send(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        return service.sendMessage(
                conversationId, jwt.getSubject(), body.get("content"));
    }
}

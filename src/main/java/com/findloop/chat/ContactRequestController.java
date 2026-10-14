
package com.findloop.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact-requests")
public class ContactRequestController {

    private final ContactRequestService service;

    public ContactRequestController(ContactRequestService service) {
        this.service = service;
    }

    @PostMapping("/{reportId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactRequest send(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long ownershipReportId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return service.send(
                    reportId, jwt.getSubject(), ownershipReportId);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(
                    ex.getStatusCode(),
                    ex.getReason());
        }
    }

    @GetMapping("/sent")
    public List<ContactRequest> sent(
            @AuthenticationPrincipal Jwt jwt) {
        return service.sentBy(jwt.getSubject());
    }

    @GetMapping("/received")
    public List<ContactRequest> received(
            @AuthenticationPrincipal Jwt jwt) {
        return service.receivedBy(jwt.getSubject());
    }

    @PatchMapping("/{requestId}")
    public ContactRequest respond(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        return service.respond(
                requestId, jwt.getSubject(), body.get("decision"));
    }
}

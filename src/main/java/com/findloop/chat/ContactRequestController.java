
package com.findloop.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact-requests")
@Tag(name = "Contact requests", description = "Send and respond to private requests about a reported item.")
@SecurityRequirement(name = "bearerAuth")
public class ContactRequestController {

    private final ContactRequestService service;

    public ContactRequestController(ContactRequestService service) {
        this.service = service;
    }

    @PostMapping("/{reportId}")
    @Operation(summary = "Send a contact request")
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
    @Operation(summary = "List sent contact requests")
    public List<ContactRequest> sent(
            @AuthenticationPrincipal Jwt jwt) {
        return service.sentBy(jwt.getSubject());
    }

    @GetMapping("/received")
    @Operation(summary = "List received contact requests")
    public List<ContactRequest> received(
            @AuthenticationPrincipal Jwt jwt) {
        return service.receivedBy(jwt.getSubject());
    }

    @PatchMapping("/{requestId}")
    @Operation(summary = "Respond to a contact request", description = "Accepts or declines a request using the decision field in the request body.")
    public ContactRequest respond(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        return service.respond(
                requestId, jwt.getSubject(), body.get("decision"));
    }
}

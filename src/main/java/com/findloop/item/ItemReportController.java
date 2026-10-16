package com.findloop.item;

import com.findloop.matching.MatchResult;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Create, find, and manage lost-and-found reports and their possible matches.")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
}, methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PATCH,
        RequestMethod.OPTIONS
}, allowedHeaders = "*")
public class ItemReportController {
    private final ItemReportService service;

    public ItemReportController(ItemReportService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a report", description = "Creates a lost or found item report. Found reports may include an ownership question and answer.")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemReportView create(@Valid @RequestBody CreateItemReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.create(request, jwt.getSubject()), jwt.getSubject());
    }

    @GetMapping
    @Operation(summary = "List or search reports", description = "Lists open reports, optionally filtered by type, text, category, or radius around coordinates.")
    public List<ItemReportView> list(@RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @AuthenticationPrincipal Jwt jwt) {
        String viewerId = jwt == null ? null : jwt.getSubject();
        if ((q != null && !q.isBlank()) || (category != null && !category.isBlank())
                || latitude != null || longitude != null || radiusKm != null) {
            return service.search(type, q, category, latitude, longitude, radiusKm).stream()
                    .map(report -> ItemReportView.from(report, viewerId)).toList();
        }
        return service.list(type).stream().map(report -> ItemReportView.from(report, viewerId)).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a report", description = "Returns the report details for the supplied report ID.")
    public ItemReportView get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.get(id), jwt == null ? null : jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update report status", description = "Changes the report status, for example to close or reopen it.")
    @SecurityRequirement(name = "bearerAuth")
    public ItemReportView updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.updateStatus(id, jwt.getSubject(), request.status()), jwt.getSubject());
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "Get possible matches", description = "Returns up to ten opposite-type reports ranked by description, location, and event time similarity.")
    public List<MatchResult> matches(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.matches(id, jwt == null ? null : jwt.getSubject());
    }

    @GetMapping("/{id}/ownership-check")
    @Operation(summary = "Get ownership question", description = "Returns the private ownership question associated with a found report, if one was provided.")
    public java.util.Map<String, String> ownershipQuestion(@PathVariable Long id) {
        return java.util.Collections.singletonMap("question", service.ownershipQuestion(id));
    }

    @PostMapping("/{id}/ownership-check")
    @Operation(summary = "Verify an ownership answer", description = "Checks an answer to the report owner's private ownership question.")
    @SecurityRequirement(name = "bearerAuth")
    public void verifyOwnership(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        service.verifyOwnership(id, jwt.getSubject(), body.get("answer"));
    }

    @PostMapping("/{id}/matches/{candidateId}/not-a-match")
    @Operation(summary = "Mark a match as not relevant", description = "Records that the candidate report is not a match for the source report.")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notAMatch(@PathVariable Long id, @PathVariable Long candidateId, @AuthenticationPrincipal Jwt jwt) {
        service.notAMatch(id, candidateId, jwt.getSubject());
    }
}

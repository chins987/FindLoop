package com.findloop.item;

import com.findloop.matching.MatchResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
@RestController
@RequestMapping("/api/reports")
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
    @ResponseStatus(HttpStatus.CREATED)
    public ItemReportView create(@Valid @RequestBody CreateItemReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.create(request, jwt.getSubject()), jwt.getSubject());
    }

    @GetMapping
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
    public ItemReportView get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.get(id), jwt == null ? null : jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    public ItemReportView updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ItemReportView.from(service.updateStatus(id, jwt.getSubject(), request.status()), jwt.getSubject());
    }

    @GetMapping("/{id}/matches")
    public List<MatchResult> matches(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.matches(id, jwt == null ? null : jwt.getSubject());
    }

    @GetMapping("/{id}/ownership-check")
    public java.util.Map<String, String> ownershipQuestion(@PathVariable Long id) {
        return java.util.Collections.singletonMap("question", service.ownershipQuestion(id));
    }

    @PostMapping("/{id}/ownership-check")
    public void verifyOwnership(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        service.verifyOwnership(id, jwt.getSubject(), body.get("answer"));
    }

    @PostMapping("/{id}/matches/{candidateId}/not-a-match")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notAMatch(@PathVariable Long id, @PathVariable Long candidateId, @AuthenticationPrincipal Jwt jwt) {
        service.notAMatch(id, candidateId, jwt.getSubject());
    }
}

package com.findloop.item;

import com.findloop.matching.HybridMatchingService;
import com.findloop.matching.MatchResult;
import com.findloop.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ItemReportService {
    private static final Logger log = LoggerFactory.getLogger(ItemReportService.class);
    private final ItemReportRepository repository;
    private final HybridMatchingService matchingService;
    private final NotificationService notificationService;
    private final OwnershipVerificationRepository ownershipVerificationRepository;
    private final MatchFeedbackRepository matchFeedbackRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ItemReportService(ItemReportRepository repository, HybridMatchingService matchingService,
            NotificationService notificationService, OwnershipVerificationRepository ownershipVerificationRepository,
            MatchFeedbackRepository matchFeedbackRepository) {
        this.repository = repository;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
        this.ownershipVerificationRepository = ownershipVerificationRepository;
        this.matchFeedbackRepository = matchFeedbackRepository;
    }

    public ItemReport create(CreateItemReportRequest request, String ownerId) {
        ItemReport report = new ItemReport(
                request.type(),
                request.title().trim(),
                request.description().trim(),
                request.category().trim(),
                request.location().trim(),
                request.eventDateTime());

        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setLocationDetails(request.locationDetails() == null || request.locationDetails().isBlank()
                ? null
                : request.locationDetails().trim());
        report.setOwnerId(ownerId);
        if (request.type() == ItemType.FOUND && request.ownershipQuestion() != null
                && !request.ownershipQuestion().isBlank()) {
            report.setOwnershipCheck(
                    request.ownershipQuestion().trim(),
                    passwordEncoder.encode(hashOwnershipAnswer(request.ownershipAnswer())));
        }

        ItemReport saved = repository.save(report);
        if (saved.getOwnerId() != null) {
            try {
                matchingService.findMatches(saved).stream()
                        .filter(match -> match.finalScore() >= 70.0)
                        .limit(3)
                        .map(match -> repository.findById(match.candidateId()).orElse(null))
                        .filter(candidate -> candidate != null && candidate.getOwnerId() != null
                                && !candidate.getOwnerId().equals(saved.getOwnerId()))
                        .forEach(candidate -> notificationService.notifyMatch(candidate.getOwnerId(), candidate.getId(),
                                saved.getTitle()));
            } catch (RuntimeException ex) {
                log.warn("Report {} was saved, but match alerts could not be created", saved.getId(), ex);
            }
        }
        return saved;
    }

    public List<ItemReport> list(String type) {
        if (type == null || type.isBlank()) {
            return repository.findByStatusIgnoreCaseOrderByCreatedAtDesc("OPEN");
        }
        return repository.findByTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(
                ItemType.valueOf(type.trim().toUpperCase()), "OPEN");
    }

    public List<ItemReport> search(String type, String query, String category,
            Double latitude, Double longitude, Double radiusKm) {
        ItemType itemType = (type == null || type.isBlank()) ? null
                : ItemType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        String cleanedQuery = query == null || query.isBlank() ? null : query.trim();
        String cleanedCategory = category == null || category.isBlank() ? null : category.trim();

        Double minLat = null, maxLat = null, minLon = null, maxLon = null;
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together.");
        }
        if (latitude == null && radiusKm != null) {
            throw new IllegalArgumentException("A search radius requires latitude and longitude.");
        }
        if (latitude != null) {
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Coordinates are outside valid ranges.");
            }
            double radius = radiusKm == null ? 10.0 : radiusKm;
            if (radius <= 0 || radius > 100) {
                throw new IllegalArgumentException("Search radius must be between 0 and 100 km.");
            }
            minLat = Math.max(-90, latitude - radius / 110.574);
            maxLat = Math.min(90, latitude + radius / 110.574);
            double longitudeDelta = radius / (111.320 * Math.max(0.01, Math.cos(Math.toRadians(latitude))));
            minLon = Math.max(-180, longitude - longitudeDelta);
            maxLon = Math.min(180, longitude + longitudeDelta);
        }

        List<ItemReport> candidates = repository.searchOpenReports(itemType, cleanedCategory, cleanedQuery,
                minLat, maxLat, minLon, maxLon);
        if (latitude == null)
            return candidates.stream().limit(200).toList();
        double radius = radiusKm == null ? 10.0 : radiusKm;
        return candidates.stream()
                .filter(report -> report.getLatitude() != null && report.getLongitude() != null)
                .filter(report -> distanceKm(latitude, longitude, report.getLatitude(),
                        report.getLongitude()) <= radius)
                .limit(200)
                .toList();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<MatchResult> matches(Long id, String viewerId) {
        ItemReport source = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        if (!"OPEN".equalsIgnoreCase(source.getStatus()))
            return List.of();
        List<MatchResult> results = matchingService.findMatches(source);
        if (viewerId == null)
            return results;
        var rejected = matchFeedbackRepository.findCandidateIdsByViewerIdAndSourceId(viewerId, id);
        return results.stream().filter(match -> !rejected.contains(match.candidateId())).toList();
    }

    public String ownershipQuestion(Long id) {
        return get(id).getOwnershipQuestion();
    }

    public void verifyOwnership(Long id, String userId, String answer) {
        ItemReport report = get(id);
        if (report.getOwnershipAnswerHash() == null)
            return;
        if (userId == null || userId.equals(report.getOwnerId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This check is for a potential claimant.");
        OwnershipVerification check = ownershipVerificationRepository.findByReportIdAndUserId(id, userId)
                .orElseGet(() -> new OwnershipVerification(id, userId));
        if (check.getVerifiedAt() != null
                && check.getVerifiedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(30)))
            return;
        if (check.getLastAttemptAt() == null
                || check.getLastAttemptAt().isBefore(java.time.LocalDateTime.now().minusHours(1)))
            check.setAttempts(0);
        if (check.getAttempts() >= 5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.");
        check.setAttempts(check.getAttempts() + 1);
        check.setLastAttemptAt(java.time.LocalDateTime.now());
        if (!passwordEncoder.matches(hashOwnershipAnswer(answer), report.getOwnershipAnswerHash())) {
            ownershipVerificationRepository.save(check);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That answer doesn't match. Please try again.");
        }
        check.setVerifiedAt(java.time.LocalDateTime.now());
        ownershipVerificationRepository.save(check);
    }

    private String hashOwnershipAnswer(String answer) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT))
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public void notAMatch(Long sourceId, Long candidateId, String viewerId) {
        if (viewerId == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        ItemReport source = get(sourceId), candidate = get(candidateId);
        if (source.getType() == candidate.getType() || !"OPEN".equalsIgnoreCase(source.getStatus())
                || !"OPEN".equalsIgnoreCase(candidate.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open lost/found pairs can be labeled.");
        Long first = Math.min(sourceId, candidateId), second = Math.max(sourceId, candidateId);
        if (!matchFeedbackRepository.existsByViewerIdAndSourceIdAndCandidateId(viewerId, first, second))
            matchFeedbackRepository.save(new MatchFeedback(viewerId, first, second));
    }

    public ItemReport get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
    }

    public ItemReport updateStatus(Long id, String viewerId, ItemStatus status) {
        ItemReport report = get(id);
        if (viewerId == null || !viewerId.equals(report.getOwnerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Only the report owner can update its status.");
        }
        if (status != ItemStatus.OPEN && status != ItemStatus.RETURNED && status != ItemStatus.CLOSED) {
            throw new IllegalArgumentException("Status must be OPEN, RETURNED, or CLOSED.");
        }
        report.setStatus(status.name());
        return repository.save(report);
    }
}

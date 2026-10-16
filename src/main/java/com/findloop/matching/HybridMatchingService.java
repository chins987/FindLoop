package com.findloop.matching;

import com.findloop.item.ItemReport;
import com.findloop.item.ItemType;
import com.findloop.item.ItemReportRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HybridMatchingService {
    // Provisional weights; validate them with labelled examples before claiming
    // accuracy.
    private static final double LOCATION_WEIGHT = 0.45;
    private static final double DESCRIPTION_WEIGHT = 0.40;
    private static final double TIME_WEIGHT = 0.15;

    private static final Map<String, Set<String>> SYNONYMS = Map.of(
            "water_bottle", Set.of("water bottle", "bottle", "flask", "drink bottle"),
            "mobile_phone", Set.of("mobile phone", "phone", "cellphone", "smartphone"),
            "dark_blue", Set.of("dark blue", "navy", "deep blue"),
            "wallet", Set.of("wallet", "purse", "billfold"),
            "earbuds", Set.of("earbuds", "earphones", "wireless earphones"));

    private static final Set<String> EVIDENCE_STOP_WORDS = Set.of(
            "the", "and", "with", "for", "from", "near", "at", "my", "a", "an", "in", "on", "of", "to",
            "is", "was", "it", "this", "that", "lost", "found", "item", "report");

    private final ItemReportRepository repository;

    public HybridMatchingService(ItemReportRepository repository) {
        this.repository = repository;
    }

    public List<MatchResult> findMatches(ItemReport source) {
        ItemType opposite = source.getType() == ItemType.LOST ? ItemType.FOUND : ItemType.LOST;
        return repository.findByTypeAndStatusIgnoreCaseOrderByCreatedAtDesc(opposite, "OPEN")
                .stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), source.getId()))
                .map(candidate -> score(source, candidate))
                .sorted(Comparator.comparingDouble(MatchResult::finalScore).reversed())
                .limit(10)
                .toList();
    }

    public MatchResult score(ItemReport a, ItemReport b) {
        double description = descriptionScore(a.getTitle() + " " + a.getDescription(),
                b.getTitle() + " " + b.getDescription());
        double geographic = geographicScore(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        double location = geographic >= 0 ? geographic : locationScore(a.getLocation(), b.getLocation());
        double time = timeScore(a.getEventDateTime(), b.getEventDateTime());
        String descriptionA = a.getTitle() + " " + a.getDescription();
        String descriptionB = b.getTitle() + " " + b.getDescription();
        List<String> sharedDetails = sharedDetails(descriptionA, descriptionB);
        Double preciseDistanceKm = distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        Double distanceKm = preciseDistanceKm == null ? null : Math.round(preciseDistanceKm * 1000.0) / 1000.0;
        boolean sameLocationText = a.getLocation() != null && b.getLocation() != null
                && !a.getLocation().isBlank() && !b.getLocation().isBlank()
                && normalize(a.getLocation()).equals(normalize(b.getLocation()));
        Long timeGapMinutes = timeGapMinutes(a.getEventDateTime(), b.getEventDateTime());

        double finalScore = LOCATION_WEIGHT * location
                + DESCRIPTION_WEIGHT * description
                + TIME_WEIGHT * time;

        String explanation = String.format(Locale.ROOT,
                "Description %.0f/100; location %.0f/100; date/time %.0f/100. Similarity is not proof of ownership.",
                description * 100, location * 100, time * 100);

        return new MatchResult(b.getId(), b.getTitle(), round(description * 100),
                round(location * 100), round(time * 100), round(finalScore * 100), explanation,
                sharedDetails, distanceKm, sameLocationText, timeGapMinutes);
    }

    private List<String> sharedDetails(String first, String second) {
        Set<String> sharedWords = tokenize(first);
        sharedWords.retainAll(tokenize(second));
        sharedWords.removeAll(EVIDENCE_STOP_WORDS);

        Set<String> sharedConcepts = concepts(first);
        sharedConcepts.retainAll(concepts(second));

        // Include recognized item concepts (for example, "wallet") plus useful
        // shared attributes, while avoiding repeated aliases such as "wallet" and
        // "purse" in the same explanation.
        Set<String> details = new HashSet<>();
        for (String concept : sharedConcepts) {
            details.add(concept.replace('_', ' '));
            SYNONYMS.get(concept).stream()
                    .flatMap(alias -> tokenize(alias).stream())
                    .forEach(sharedWords::remove);
        }
        details.addAll(sharedWords);
        return details.stream().sorted().limit(4).toList();
    }

    private Long timeGapMinutes(LocalDateTime first, LocalDateTime second) {
        if (first == null || second == null) return null;
        return Math.abs(Duration.between(first, second).toMinutes());
    }

    private Double distanceKm(Double latitudeA, Double longitudeA, Double latitudeB, Double longitudeB) {
        if (latitudeA == null || longitudeA == null || latitudeB == null || longitudeB == null) return null;
        if (!Double.isFinite(latitudeA) || !Double.isFinite(longitudeA)
                || !Double.isFinite(latitudeB) || !Double.isFinite(longitudeB)
                || Math.abs(latitudeA) > 90 || Math.abs(latitudeB) > 90
                || Math.abs(longitudeA) > 180 || Math.abs(longitudeB) > 180) return null;
        double lat1 = Math.toRadians(latitudeA), lat2 = Math.toRadians(latitudeB);
        double dLat = lat2 - lat1, dLon = Math.toRadians(longitudeB - longitudeA);
        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        haversine = Math.max(0.0, Math.min(1.0, haversine));
        double km = 6371.0088 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return km;
    }

    // Keyword overlap + curated synonym concepts; semantic embeddings are a later
    // integration.

    public double descriptionScore(String first, String second) {
        String normalizedA = normalize(first);
        String normalizedB = normalize(second);

        // Identical non-empty descriptions should receive a perfect score.
        if (!normalizedA.isEmpty() && normalizedA.equals(normalizedB)) {
            return 1.0;
        }

        Set<String> wordsA = tokenize(normalizedA);
        Set<String> wordsB = tokenize(normalizedB);
        double keyword = jaccard(wordsA, wordsB);

        Set<String> conceptsA = concepts(normalizedA);
        Set<String> conceptsB = concepts(normalizedB);
        double concept = jaccard(conceptsA, conceptsB);

        double score = (conceptsA.isEmpty() && conceptsB.isEmpty())
                ? keyword
                : 0.60 * concept + 0.40 * keyword;

        return clamp(score);
    }

    private Set<String> concepts(String text) {
        String normalized = normalize(text);
        Set<String> found = new HashSet<>();
        // Longest phrases first helps prefer "water bottle" over "bottle".
        List<Map.Entry<String, Set<String>>> groups = new ArrayList<>(SYNONYMS.entrySet());
        groups.sort((a, b) -> Integer.compare(
                b.getValue().stream().mapToInt(String::length).max().orElse(0),
                a.getValue().stream().mapToInt(String::length).max().orElse(0)));
        for (var group : groups) {
            for (String phrase : group.getValue()) {
                String p = normalize(phrase);
                if (Pattern.compile("(^| )" + Pattern.quote(p) + "( |$)").matcher(normalized).find()) {
                    found.add(group.getKey());
                    break;
                }
            }
        }
        return found;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(normalize(text).split(" "))
                .filter(s -> !s.isBlank() && s.length() > 1)
                .collect(Collectors.toSet());
    }

    private String normalize(String text) {
        return text == null ? ""
                : text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .trim().replaceAll("\\s+", " ");
    }

    private double locationScore(String first, String second) {
        if (first == null || second == null || first.isBlank() || second.isBlank())
            return 0.5;
        String a = normalize(first);
        String b = normalize(second);
        if (a.equals(b))
            return 1.0;
        Set<String> wa = tokenize(a), wb = tokenize(b);
        double overlap = jaccard(wa, wb);
        // Unknown/ambiguous text locations should not be presented as precise
        // geographic distance.
        return Math.max(0.25, overlap);
    }

    public double geographicScore(Double latitudeA, Double longitudeA,
            Double latitudeB, Double longitudeB) {
        if (latitudeA == null || longitudeA == null || latitudeB == null || longitudeB == null) {
            return -1.0;
        }
        Double calculatedDistanceKm = distanceKm(latitudeA, longitudeA, latitudeB, longitudeB);
        if (calculatedDistanceKm == null) return -1.0;
        double distanceKm = calculatedDistanceKm;
        if (distanceKm <= 0.25) return 1.0;
        if (distanceKm <= 1.0) return 0.9;
        if (distanceKm <= 3.0) return 0.7;
        if (distanceKm <= 10.0) return 0.4;
        if (distanceKm <= 25.0) return 0.2;
        return 0.0;
    }

    private double timeScore(LocalDateTime first, LocalDateTime second) {
        // Unknown event time is neutral, not a mismatch. Report submission time is not
        // used.
        if (first == null || second == null)
            return 0.5;
        long hours = Math.abs(Duration.between(first, second).toHours());
        if (hours <= 24)
            return 1.0;
        if (hours <= 72)
            return 0.8;
        if (hours <= 168)
            return 0.6;
        if (hours <= 720)
            return 0.3;
        return 0.1;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty())
            return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

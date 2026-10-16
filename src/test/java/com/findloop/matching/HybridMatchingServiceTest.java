
package com.findloop.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.findloop.item.ItemReport;
import com.findloop.item.ItemReportRepository;
import com.findloop.item.ItemType;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

class HybridMatchingServiceTest {

    private HybridMatchingService matchingService;
    private List<ItemReport> repositoryReports;
    private ItemType requestedType;
    private String requestedStatus;

    @BeforeEach
    void setUp() {
        repositoryReports = new ArrayList<>();
        ItemReportRepository repository = (ItemReportRepository) Proxy.newProxyInstance(
                ItemReportRepository.class.getClassLoader(),
                new Class<?>[] { ItemReportRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("findByTypeAndStatusIgnoreCaseOrderByCreatedAtDesc")) {
                        requestedType = (ItemType) args[0];
                        requestedStatus = (String) args[1];
                        return repositoryReports;
                    }
                    if (method.getName().equals("toString")) return "TestItemReportRepository";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    return null;
                });
        matchingService = new HybridMatchingService(repository);
    }

    @Test
    void findMatchesShouldRequestOpenReportsOfOppositeType() {
        ItemReport lost = testReport(ItemType.LOST, 1L, "Black Wallet", "Black leather wallet",
                "College Library", LocalDateTime.of(2026, 10, 10, 14, 0));
        ItemReport found = testReport(ItemType.FOUND, 2L, "Black Wallet", "Black leather wallet",
                "College Library", LocalDateTime.of(2026, 10, 10, 14, 0));
        repositoryReports.add(found);

        List<MatchResult> results = matchingService.findMatches(lost);

        assertEquals(ItemType.FOUND, requestedType);
        assertEquals("OPEN", requestedStatus);
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).candidateId());
    }

    @Test
    void findMatchesShouldExcludeTheSourceReport() {
        ItemReport source = testReport(ItemType.LOST, 1L, "Black Wallet", "Black leather wallet",
                "College Library", LocalDateTime.of(2026, 10, 10, 14, 0));
        ItemReport candidate = testReport(ItemType.FOUND, 2L, "Black Wallet", "Black leather wallet",
                "College Library", LocalDateTime.of(2026, 10, 10, 14, 0));
        repositoryReports.add(source);
        repositoryReports.add(candidate);

        List<MatchResult> results = matchingService.findMatches(source);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).candidateId());
    }

    @Test
    void findMatchesShouldRankResultsAndReturnAtMostTen() {
        LocalDateTime eventTime = LocalDateTime.of(2026, 10, 10, 14, 0);
        ItemReport source = testReport(ItemType.LOST, 1L, "Black Wallet", "Black leather wallet",
                "College Library", eventTime);
        ItemReport best = testReport(ItemType.FOUND, 2L, "Black Wallet", "Black leather wallet",
                "College Library", eventTime);
        repositoryReports.add(best);

        for (long id = 3; id <= 13; id++) {
            repositoryReports.add(testReport(ItemType.FOUND, id, "Red Backpack", "Large red school backpack",
                    "Airport Parking", eventTime.minusMonths(2)));
        }

        List<MatchResult> results = matchingService.findMatches(source);

        assertEquals(10, results.size());
        assertEquals(2L, results.get(0).candidateId(), "The strongest match should rank first");
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).finalScore() >= results.get(i).finalScore(),
                    "Results should be sorted from highest to lowest score");
        }
    }

    private ItemReport testReport(ItemType type, long id, String title, String description,
            String location, LocalDateTime eventTime) {
        ItemReport report = new ItemReport(type, title, description, "Other", location, eventTime);
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    @Test
    void identicalNormalizedDescriptionsShouldScoreOne() {
        double score = matchingService.descriptionScore(
                "Black Leather-Wallet",
                " black   leather wallet ");

        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void emptyDescriptionsShouldScoreZero() {
        double score = matchingService.descriptionScore("", "");

        assertEquals(0.0, score, 0.0001);
    }

    @Test
    void nullAndBlankDescriptionsShouldScoreZero() {
        assertEquals(0.0, matchingService.descriptionScore(null, null), 0.0001);
        assertEquals(0.0, matchingService.descriptionScore(null, "black wallet"), 0.0001);
        assertEquals(0.0, matchingService.descriptionScore("---", "   "), 0.0001);
    }

    @Test
    void walletSynonymsShouldImproveDescriptionSimilarity() {
        double score = matchingService.descriptionScore(
                "wallet",
                "purse");

        assertTrue(score > 0.0,
                "Recognized wallet synonyms should produce a positive score");
    }

    @Test
    void unrelatedDescriptionsShouldHaveLowSimilarity() {
        double score = matchingService.descriptionScore(
                "blue school bag",
                "red leather wallet");

        assertTrue(score < 0.30,
                "Unrelated descriptions should have low similarity");
    }

    @Test
    void identicalCoordinatesShouldScoreOne() {
        double score = matchingService.geographicScore(
                17.3850, 78.4867,
                17.3850, 78.4867);

        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void nearbyCoordinatesShouldReceiveHighScore() {
        double score = matchingService.geographicScore(
                17.3850, 78.4867,
                17.3860, 78.4867);

        assertTrue(score >= 0.9,
                "Locations within approximately 250 metres should score highly");
    }

    @Test
    void distantCoordinatesShouldScoreZero() {
        double score = matchingService.geographicScore(
                0.0, 0.0,
                0.0, 1.0);

        assertEquals(0.0, score, 0.0001);
    }

    @Test
    void missingCoordinatesShouldReturnFallbackValue() {
        double score = matchingService.geographicScore(
                null, null,
                17.3850, 78.4867);

        assertEquals(-1.0, score, 0.0001);
        assertEquals(-1.0, matchingService.geographicScore(
                17.3850, null, 17.3850, 78.4867), 0.0001);
    }

    @Test
    void invalidCoordinatesShouldReturnFallbackValue() {
        assertEquals(-1.0, matchingService.geographicScore(
                91.0, 0.0, 0.0, 0.0), 0.0001);
        assertEquals(-1.0, matchingService.geographicScore(
                0.0, 181.0, 0.0, 0.0), 0.0001);
    }

    @Test
    void geographicScoreShouldChangeAtDistanceBandBoundaries() {
        assertEquals(1.0, geographicScoreAtDistanceKm(0.249), 0.0001);
        assertEquals(0.9, geographicScoreAtDistanceKm(0.251), 0.0001);
        assertEquals(0.9, geographicScoreAtDistanceKm(0.999), 0.0001);
        assertEquals(0.7, geographicScoreAtDistanceKm(1.001), 0.0001);
        assertEquals(0.7, geographicScoreAtDistanceKm(2.999), 0.0001);
        assertEquals(0.4, geographicScoreAtDistanceKm(3.001), 0.0001);
        assertEquals(0.4, geographicScoreAtDistanceKm(9.999), 0.0001);
        assertEquals(0.2, geographicScoreAtDistanceKm(10.001), 0.0001);
        assertEquals(0.2, geographicScoreAtDistanceKm(24.999), 0.0001);
        assertEquals(0.0, geographicScoreAtDistanceKm(25.001), 0.0001);
        // Evidence rounds to metres, but scoring must use the precise distance.
        assertEquals(0.9, geographicScoreAtDistanceKm(0.2504), 0.0001);
    }

    private double geographicScoreAtDistanceKm(double distanceKm) {
        double longitude = Math.toDegrees(distanceKm / 6371.0088);
        return matchingService.geographicScore(0.0, 0.0, 0.0, longitude);
    }

    @Test
    void identicalReportsWithSameLocationAndTimeShouldScore100() {
        LocalDateTime eventTime = LocalDateTime.of(2026, 10, 10, 14, 0);

        ItemReport lost = new ItemReport(
                ItemType.LOST,
                "Black Wallet",
                "Black leather wallet",
                "Wallet",
                "College Library",
                eventTime);

        ItemReport found = new ItemReport(
                ItemType.FOUND,
                "Black Wallet",
                "Black leather wallet",
                "Wallet",
                "College Library",
                eventTime);

        MatchResult result = matchingService.score(lost, found);

        assertEquals(100.0, result.descriptionScore(), 0.001);
        assertEquals(100.0, result.locationScore(), 0.001);
        assertEquals(100.0, result.dateTimeScore(), 0.001);
        assertEquals(100.0, result.finalScore(), 0.001);
        assertTrue(result.sharedDetails().contains("wallet"));
        assertTrue(result.sharedDetails().contains("black"));
        assertTrue(result.sharedDetails().contains("leather"));
        assertTrue(result.sameLocationText());
        assertEquals(0L, result.timeGapMinutes());
        assertNull(result.distanceKm(), "Distance should be absent when coordinates are unavailable");
    }

    @Test
    void missingEventTimeShouldReceiveNeutralScore() {
        ItemReport lost = new ItemReport(ItemType.LOST, "Black Wallet", "Black leather wallet",
                "Wallet", "College Library", null);
        ItemReport found = new ItemReport(ItemType.FOUND, "Black Wallet", "Black leather wallet",
                "Wallet", "College Library", LocalDateTime.of(2026, 10, 10, 14, 0));

        MatchResult oneTimeMissing = matchingService.score(lost, found);
        assertEquals(50.0, oneTimeMissing.dateTimeScore(), 0.001);
        assertNull(oneTimeMissing.timeGapMinutes());

        MatchResult bothTimesMissing = matchingService.score(lost,
                new ItemReport(ItemType.FOUND, "Black Wallet", "Black leather wallet",
                        "Wallet", "College Library", null));
        assertEquals(50.0, bothTimesMissing.dateTimeScore(), 0.001);
        assertNull(bothTimesMissing.timeGapMinutes());
    }

    @Test
    void identicalDescriptionsWithDistantCoordinatesShouldScore55() {
        LocalDateTime eventTime = LocalDateTime.of(2026, 10, 10, 14, 0);

        ItemReport lost = new ItemReport(
                ItemType.LOST,
                "Black Wallet",
                "Black leather wallet",
                "Wallet",
                "College Library",
                eventTime);

        ItemReport found = new ItemReport(
                ItemType.FOUND,
                "Black Wallet",
                "Black leather wallet",
                "Wallet",
                "College Library",
                eventTime);

        lost.setLatitude(0.0);
        lost.setLongitude(0.0);

        found.setLatitude(0.0);
        found.setLongitude(1.0);

        MatchResult result = matchingService.score(lost, found);

        assertEquals(100.0, result.descriptionScore(), 0.001);
        assertEquals(0.0, result.locationScore(), 0.001);
        assertEquals(100.0, result.dateTimeScore(), 0.001);
        assertEquals(55.0, result.finalScore(), 0.001);
        assertTrue(result.distanceKm() > 100.0);
    }

}

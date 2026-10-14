
package com.findloop.matching;

import com.findloop.item.ItemReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.findloop.item.ItemReport;
import com.findloop.item.ItemType;
import java.time.LocalDateTime;

class HybridMatchingServiceTest {

    private HybridMatchingService matchingService;

    @BeforeEach
    void setUp() {
        ItemReportRepository repository = mock(ItemReportRepository.class);
        matchingService = new HybridMatchingService(repository);
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
    }

}

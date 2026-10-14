package com.findloop.matching;

import com.findloop.item.ItemReport;
import com.findloop.item.ItemType;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingRankingEvaluationTest {
    private record Example(ItemReport lost, ItemReport found, boolean relevant) {}

    @Test
    void reportsRecallAtThreeAndMeanReciprocalRankOnLabeledExamples() throws Exception {
        List<Example> examples;
        try (var stream = getClass().getResourceAsStream("/matching-evaluation.psv")) {
            if (stream == null) throw new IllegalStateException("Missing matching-evaluation.psv");
            examples = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines().filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(this::parse).toList();
        }

        Map<String, List<Example>> groups = examples.stream().collect(Collectors.groupingBy(
                e -> e.lost().getTitle(), LinkedHashMap::new, Collectors.toList()));
        HybridMatchingService matcher = new HybridMatchingService(null);
        int foundInTopThree = 0;
        double reciprocalRankTotal = 0;
        for (List<Example> group : groups.values()) {
            List<Example> ranked = group.stream().sorted(Comparator.comparingDouble(
                    (Example e) -> matcher.score(e.lost(), e.found()).finalScore()).reversed()).toList();
            int rank = 0;
            for (int i = 0; i < ranked.size(); i++) {
                if (ranked.get(i).relevant()) { rank = i + 1; break; }
            }
            if (rank > 0 && rank <= 3) foundInTopThree++;
            if (rank > 0) reciprocalRankTotal += 1.0 / rank;
        }
        double recallAtThree = (double) foundInTopThree / groups.size();
        double meanReciprocalRank = reciprocalRankTotal / groups.size();
        System.out.printf(Locale.ROOT, "Synthetic matching evaluation: cases=%d, Recall@3=%.2f, MRR=%.2f%n",
                groups.size(), recallAtThree, meanReciprocalRank);
        assertTrue(recallAtThree >= 0.80, "At least 80% of labeled matches should appear in the top three");
        assertTrue(meanReciprocalRank >= 0.60, "Mean reciprocal rank should be at least 0.60");
    }

    private Example parse(String line) {
        String[] f = line.split("\\|", -1);
        ItemReport lost = report(ItemType.LOST, f[1], f[2], f[3], f[4], f[5], f[6], f[7]);
        ItemReport found = report(ItemType.FOUND, f[8], f[9], f[10], f[11], f[12], f[13], f[14]);
        return new Example(lost, found, Boolean.parseBoolean(f[15]));
    }

    private ItemReport report(ItemType type, String title, String description, String category,
            String location, String latitude, String longitude, String date) {
        ItemReport report = new ItemReport(type, title, description, category, location, LocalDateTime.parse(date));
        report.setLatitude(Double.valueOf(latitude));
        report.setLongitude(Double.valueOf(longitude));
        return report;
    }
}

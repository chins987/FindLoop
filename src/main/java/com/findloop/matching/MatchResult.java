package com.findloop.matching;

import java.util.List;

public record MatchResult(
        Long candidateId,
        String candidateTitle,
        double descriptionScore,
        double locationScore,
        double dateTimeScore,
        double finalScore,
        String explanation,
        List<String> sharedDetails,
        Double distanceKm,
        boolean sameLocationText,
        Long timeGapMinutes
) {}

package com.findloop.matching;

public record MatchResult(
        Long candidateId,
        String candidateTitle,
        double descriptionScore,
        double locationScore,
        double dateTimeScore,
        double finalScore,
        String explanation
) {}

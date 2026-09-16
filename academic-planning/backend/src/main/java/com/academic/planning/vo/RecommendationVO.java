package com.academic.planning.vo;

public record RecommendationVO(
        String category,
        long gap,
        int scoreGap,
        long matchGap,
        SchoolSummaryVO school,
        RecommendedMajorVO major,
        String reason
) {}

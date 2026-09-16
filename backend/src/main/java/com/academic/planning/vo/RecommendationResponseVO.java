package com.academic.planning.vo;

import java.util.List;
import java.util.Map;

public record RecommendationResponseVO(
        String message,
        Map<String, List<RecommendationVO>> recommendations,
        String disclaimer
) {}

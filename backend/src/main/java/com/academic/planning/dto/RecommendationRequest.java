package com.academic.planning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecommendationRequest(
        @NotBlank @Size(min = 2, max = 20) String province,
        @NotBlank @Pattern(regexp = "物理类|历史类", message = "只能是物理类或历史类") String subjectType,
        @Min(0) @Max(750) int score,
        @Min(1) @Max(10_000_000) long rank,
        @Size(max = 50) String majorPreference,
        @Size(max = 50) String regionPreference
) {}

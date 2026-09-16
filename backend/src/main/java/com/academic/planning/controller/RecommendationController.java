package com.academic.planning.controller;

import com.academic.planning.common.ApiResponse;
import com.academic.planning.security.RequirePermission;
import com.academic.planning.dto.RecommendationRequest;
import com.academic.planning.service.RecommendationService;
import com.academic.planning.vo.RecommendationResponseVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @RequirePermission("recommend:use")
    @PostMapping("/recommend")
    public ApiResponse<RecommendationResponseVO> recommend(
            @Valid @RequestBody RecommendationRequest request
    ) {
        return ApiResponse.success(recommendationService.recommend(request));
    }
}

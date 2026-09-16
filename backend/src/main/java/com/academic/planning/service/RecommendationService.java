package com.academic.planning.service;

import com.academic.planning.dto.RecommendationRequest;
import com.academic.planning.vo.RecommendationResponseVO;

public interface RecommendationService {
    RecommendationResponseVO recommend(RecommendationRequest request);
}

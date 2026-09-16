package com.academic.planning.service;

import com.academic.planning.dto.RecommendationRequest;
import com.academic.planning.mapper.RecommendationMapper;
import com.academic.planning.mapper.row.RecommendationCandidateRow;
import com.academic.planning.service.impl.RecommendationServiceImpl;
import com.academic.planning.vo.RecommendationResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private RecommendationMapper mapper;
    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(RecommendationMapper.class);
        RedisCacheService cache = mock(RedisCacheService.class);
        when(cache.get(anyString(), any())).thenReturn(Optional.empty());
        service = new RecommendationServiceImpl(mapper, cache);
    }

    @Test
    void shouldClassifyByRankGapOnly() {
        when(mapper.selectCandidates(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(candidate("冲刺大学", 12_000),
                        candidate("稳妥大学", 15_000), candidate("保底大学", 19_000)));
        RecommendationRequest lowScore = new RecommendationRequest(
                "福建", "物理类", 100, 15_000, "计算机", null
        );
        RecommendationRequest highScore = new RecommendationRequest(
                "福建", "物理类", 750, 15_000, "计算机", null
        );

        RecommendationResponseVO low = service.recommend(lowScore);
        RecommendationResponseVO high = service.recommend(highScore);

        assertFalse(low.recommendations().get("冲").isEmpty());
        assertFalse(low.recommendations().get("稳").isEmpty());
        assertFalse(low.recommendations().get("保").isEmpty());
        assertEquals(low.recommendations().get("冲").get(0).category(),
                high.recommendations().get("冲").get(0).category());
        assertEquals(-3_000, low.recommendations().get("冲").get(0).matchGap());
        assertEquals(0, low.recommendations().get("稳").get(0).matchGap());
        assertEquals(4_000, low.recommendations().get("保").get(0).matchGap());
    }

    @Test
    void categoryBoundariesShouldMatchPlan() {
        assertEquals("冲", service.categoryForGap(-2_001));
        assertEquals("稳", service.categoryForGap(-2_000));
        assertEquals("稳", service.categoryForGap(2_000));
        assertEquals("保", service.categoryForGap(2_001));
    }

    private RecommendationCandidateRow candidate(String schoolName, long minRank) {
        RecommendationCandidateRow row = new RecommendationCandidateRow();
        row.setSchoolId(minRank);
        row.setSchoolName(schoolName);
        row.setSchoolProvince("福建");
        row.setCity("福州");
        row.setLevel("本科");
        row.setDescription("测试院校");
        row.setMajorName("计算机科学与技术");
        row.setMinScore(580);
        row.setMinRank(minRank);
        return row;
    }
}

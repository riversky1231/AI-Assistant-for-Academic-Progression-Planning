package com.academic.planning.service.impl;

import com.academic.planning.dto.RecommendationRequest;
import com.academic.planning.mapper.RecommendationMapper;
import com.academic.planning.mapper.row.RecommendationCandidateRow;
import com.academic.planning.vo.RecommendationResponseVO;
import com.academic.planning.vo.RecommendationVO;
import com.academic.planning.vo.RecommendedMajorVO;
import com.academic.planning.vo.SchoolSummaryVO;
import com.academic.planning.service.RedisCacheService;
import com.academic.planning.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int CATEGORY_LIMIT = 5;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Map<String, List<String>> REGION_ALIASES = Map.of(
            "江浙沪", List.of("江苏", "浙江", "上海"),
            "长三角", List.of("江苏", "浙江", "上海")
    );

    private final RecommendationMapper recommendationMapper;
    private final RedisCacheService cacheService;

    public RecommendationServiceImpl(RecommendationMapper recommendationMapper, RedisCacheService cacheService) {
        this.recommendationMapper = recommendationMapper;
        this.cacheService = cacheService;
    }

    public RecommendationResponseVO recommend(RecommendationRequest request) {
        List<String> regions = expandRegions(request.regionPreference());
        String cacheKey = cacheKey(request, regions);
        return cacheService.get(cacheKey, RecommendationResponseVO.class).orElseGet(() -> {
            List<RecommendationCandidateRow> candidates = recommendationMapper.selectCandidates(
                    request.province().trim(), request.subjectType().trim(),
                    normalize(request.majorPreference()), regions
            );
            Map<String, List<RecommendationVO>> grouped = new LinkedHashMap<>();
            grouped.put("冲", new ArrayList<>());
            grouped.put("稳", new ArrayList<>());
            grouped.put("保", new ArrayList<>());
            for (RecommendationCandidateRow row : candidates) {
                long rankGap = row.getMinRank() - request.rank();
                int scoreGap = request.score() - row.getMinScore();
                String category = categoryForGap(rankGap);
                String reason = "历史最低分 %d、最低位次 %d；您的位次差 %+d 名，按位次判定为“%s”。"
                        .formatted(row.getMinScore(), row.getMinRank(), rankGap, category);
                grouped.get(category).add(new RecommendationVO(
                        category,
                        rankGap,
                        scoreGap,
                        rankGap,
                        new SchoolSummaryVO(row.getSchoolId(), row.getSchoolName(), row.getSchoolProvince(),
                                row.getCity(), row.getLevel(), row.getDescription()),
                        new RecommendedMajorVO(row.getMajorName(), row.getMinScore(), row.getMinRank()),
                        reason
                ));
            }
            grouped.replaceAll((category, values) -> values.stream()
                    .sorted(Comparator.comparingLong(item -> Math.abs(item.matchGap())))
                    .limit(CATEGORY_LIMIT)
                    .toList());
            boolean hasResults = grouped.values().stream().anyMatch(values -> !values.isEmpty());
            RecommendationResponseVO response = new RecommendationResponseVO(
                    hasResults ? "已按历史最低位次生成冲、稳、保建议。" : "暂无符合条件的数据。",
                    grouped,
                    "推荐基于本地演示数据与规则，仅供参考，不承诺录取结果。"
            );
            cacheService.put(cacheKey, response, CACHE_TTL);
            return response;
        });
    }

    public String categoryForGap(long gap) {
        if (gap < -2_000) {
            return "冲";
        }
        if (gap > 2_000) {
            return "保";
        }
        return "稳";
    }

    private List<String> expandRegions(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return List.of();
        }
        String compact = normalized.replace(" ", "");
        if (REGION_ALIASES.containsKey(compact)) {
            return REGION_ALIASES.get(compact);
        }
        return List.of(compact.split("[、,，/]+")).stream()
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    private String cacheKey(RecommendationRequest request, List<String> regions) {
        String source = String.join("|", request.province().trim(), request.subjectType().trim(),
                String.valueOf(request.score()), String.valueOf(request.rank()),
                String.valueOf(normalize(request.majorPreference())), String.join(",", regions));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return "academic:recommend:" + HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成推荐缓存键", exception);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

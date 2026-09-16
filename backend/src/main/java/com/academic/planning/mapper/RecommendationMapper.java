package com.academic.planning.mapper;

import com.academic.planning.mapper.row.RecommendationCandidateRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecommendationMapper {
    List<RecommendationCandidateRow> selectCandidates(
            @Param("province") String province,
            @Param("subjectType") String subjectType,
            @Param("majorPreference") String majorPreference,
            @Param("regions") List<String> regions
    );
}

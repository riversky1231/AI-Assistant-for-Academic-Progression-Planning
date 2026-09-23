package com.academic.planning.vo;

public record AgentAdmissionVO(
        String province,
        String subjectType,
        Integer year,
        Integer minScore,
        Long minRank,
        String majorName,
        String category
) {}

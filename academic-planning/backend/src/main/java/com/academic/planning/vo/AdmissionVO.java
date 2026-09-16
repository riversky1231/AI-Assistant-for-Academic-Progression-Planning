package com.academic.planning.vo;

public record AdmissionVO(
        String province,
        String subjectType,
        Integer year,
        Integer minScore,
        Long minRank,
        MajorVO major
) {}

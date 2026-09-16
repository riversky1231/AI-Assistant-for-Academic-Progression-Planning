package com.academic.planning.vo;

public record SchoolSummaryVO(
        Long id,
        String name,
        String province,
        String city,
        String level,
        String description
) {}

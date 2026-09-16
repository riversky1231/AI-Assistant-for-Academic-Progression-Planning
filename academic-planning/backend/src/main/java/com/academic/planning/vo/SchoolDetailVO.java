package com.academic.planning.vo;

import java.util.List;

public record SchoolDetailVO(
        Long id,
        String name,
        String province,
        String city,
        String level,
        String description,
        List<AdmissionVO> admissions
) {}

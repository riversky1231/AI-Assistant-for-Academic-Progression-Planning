package com.academic.planning.vo;

import java.util.List;

/** Compact database projection intended only for the LLM tool context. */
public record AgentSchoolDetailVO(
        Long id,
        String name,
        String province,
        String city,
        String level,
        List<AgentAdmissionVO> admissions
) {}

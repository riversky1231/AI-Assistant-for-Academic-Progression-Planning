package com.academic.planning.controller;

import com.academic.planning.common.ApiResponse;
import com.academic.planning.security.RequirePermission;
import com.academic.planning.service.SchoolService;
import com.academic.planning.vo.SchoolDetailVO;
import com.academic.planning.vo.SchoolSummaryVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @RequirePermission("school:read")
    @GetMapping("/schools")
    public ApiResponse<List<SchoolSummaryVO>> list(
            @RequestParam(required = false) @Size(min = 1, max = 50) String keyword,
            @RequestParam(required = false) @Size(min = 2, max = 20) String province,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return ApiResponse.success(schoolService.list(keyword, province, limit));
    }

    @RequirePermission("school:read")
    @GetMapping("/schools/{schoolId}")
    public ApiResponse<SchoolDetailVO> detail(@PathVariable @Min(1) long schoolId) {
        return ApiResponse.success(schoolService.detail(schoolId));
    }
}

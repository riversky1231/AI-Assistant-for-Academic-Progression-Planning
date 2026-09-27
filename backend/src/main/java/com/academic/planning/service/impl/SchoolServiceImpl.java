package com.academic.planning.service.impl;

import com.academic.planning.common.BusinessException;
import com.academic.planning.entity.School;
import com.academic.planning.mapper.SchoolMapper;
import com.academic.planning.mapper.row.AdmissionDetailRow;
import com.academic.planning.mapper.row.AgentAdmissionRow;
import com.academic.planning.mapper.row.AgentSchoolRow;
import com.academic.planning.vo.AgentAdmissionVO;
import com.academic.planning.vo.AgentSchoolDetailVO;
import com.academic.planning.vo.AdmissionVO;
import com.academic.planning.vo.MajorVO;
import com.academic.planning.vo.SchoolDetailVO;
import com.academic.planning.vo.SchoolSummaryVO;
import com.academic.planning.service.RedisCacheService;
import com.academic.planning.service.SchoolService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 院校查询核心实现：列表按关键词/省份筛选，详情关联专业与历史录取。
 * 详情结果缓存 10 分钟，避免频繁查库。
 */
@Service
public class SchoolServiceImpl implements SchoolService {

    // 详情缓存时长
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final SchoolMapper schoolMapper;
    private final RedisCacheService cacheService;

    public SchoolServiceImpl(SchoolMapper schoolMapper, RedisCacheService cacheService) {
        this.schoolMapper = schoolMapper;
        this.cacheService = cacheService;
    }

    public List<SchoolSummaryVO> list(String keyword, String province, int limit) {
        return schoolMapper.selectSchools(normalize(keyword), normalize(province), limit)
                .stream().map(this::toSummary).toList();
    }

    public SchoolDetailVO detail(long schoolId) {
        String cacheKey = "academic:school:" + schoolId;
        // 命中缓存直接返回；否则查库并回填缓存
        return cacheService.get(cacheKey, SchoolDetailVO.class).orElseGet(() -> {
            School school = schoolMapper.selectById(schoolId);
            if (school == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "未找到该院校");
            }
            // 关联查询该院校各专业的录取详情（按年份倒序）
            List<AdmissionVO> admissions = schoolMapper.selectAdmissionDetails(schoolId)
                    .stream().map(this::toAdmission).toList();
            SchoolDetailVO detail = new SchoolDetailVO(
                    school.getId(), school.getName(), school.getProvince(), school.getCity(),
                    school.getLevel(), school.getDescription(), admissions
            );
            cacheService.put(cacheKey, detail, CACHE_TTL);
            return detail;
        });
    }

    @Override
    public AgentSchoolDetailVO agentDetail(long schoolId) {
        String cacheKey = "academic:agent:school:" + schoolId;
        return cacheService.get(cacheKey, AgentSchoolDetailVO.class).orElseGet(() -> {
            AgentSchoolRow school = schoolMapper.selectAgentSchool(schoolId);
            if (school == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "未找到该院校");
            }
            List<AgentAdmissionVO> admissions = schoolMapper.selectAgentAdmissionDetails(schoolId)
                    .stream().map(this::toAgentAdmission).toList();
            AgentSchoolDetailVO detail = new AgentSchoolDetailVO(
                    school.getId(), school.getName(), school.getProvince(), school.getCity(), school.getLevel(), admissions);
            cacheService.put(cacheKey, detail, CACHE_TTL);
            return detail;
        });
    }

    private SchoolSummaryVO toSummary(School school) {
        return new SchoolSummaryVO(school.getId(), school.getName(), school.getProvince(),
                school.getCity(), school.getLevel(), school.getDescription());
    }

    private AdmissionVO toAdmission(AdmissionDetailRow row) {
        MajorVO major = new MajorVO(row.getMajorId(), row.getMajorName(), row.getCategory(), row.getDescription());
        return new AdmissionVO(row.getProvince(), row.getSubjectType(), row.getYear(),
                row.getMinScore(), row.getMinRank(), major);
    }

    private AgentAdmissionVO toAgentAdmission(AgentAdmissionRow row) {
        return new AgentAdmissionVO(row.getProvince(), row.getSubjectType(), row.getYear(), row.getMinScore(),
                row.getMinRank(), row.getMajorName(), row.getCategory());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

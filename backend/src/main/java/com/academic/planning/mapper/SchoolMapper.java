package com.academic.planning.mapper;

import com.academic.planning.entity.School;
import com.academic.planning.mapper.row.AdmissionDetailRow;
import com.academic.planning.mapper.row.AgentAdmissionRow;
import com.academic.planning.mapper.row.AgentSchoolRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SchoolMapper extends BaseMapper<School> {
    List<School> selectSchools(
            @Param("keyword") String keyword,
            @Param("province") String province,
            @Param("limit") int limit
    );

    List<AdmissionDetailRow> selectAdmissionDetails(@Param("schoolId") long schoolId);

    AgentSchoolRow selectAgentSchool(@Param("schoolId") long schoolId);
    List<AgentAdmissionRow> selectAgentAdmissionDetails(@Param("schoolId") long schoolId);
}

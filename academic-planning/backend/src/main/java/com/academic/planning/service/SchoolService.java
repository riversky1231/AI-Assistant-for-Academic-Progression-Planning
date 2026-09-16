package com.academic.planning.service;

import com.academic.planning.vo.SchoolDetailVO;
import com.academic.planning.vo.SchoolSummaryVO;

import java.util.List;

public interface SchoolService {
    List<SchoolSummaryVO> list(String keyword, String province, int limit);
    SchoolDetailVO detail(long schoolId);
}

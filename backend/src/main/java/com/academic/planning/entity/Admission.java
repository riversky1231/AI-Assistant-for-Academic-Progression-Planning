package com.academic.planning.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("admission")
public class Admission {
    private Long id;
    private Long schoolId;
    private Long majorId;
    private String province;
    private String subjectType;
    private Integer year;
    private Integer minScore;
    private Long minRank;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getMinScore() { return minScore; }
    public void setMinScore(Integer minScore) { this.minScore = minScore; }
    public Long getMinRank() { return minRank; }
    public void setMinRank(Long minRank) { this.minRank = minRank; }
}

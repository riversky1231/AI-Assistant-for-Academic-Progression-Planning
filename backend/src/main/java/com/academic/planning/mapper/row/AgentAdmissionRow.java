package com.academic.planning.mapper.row;

public class AgentAdmissionRow {
    private String province;
    private String subjectType;
    private Integer year;
    private Integer minScore;
    private Long minRank;
    private String majorName;
    private String category;

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
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}

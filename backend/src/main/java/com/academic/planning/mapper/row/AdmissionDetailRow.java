package com.academic.planning.mapper.row;

public class AdmissionDetailRow {
    private String province;
    private String subjectType;
    private Integer year;
    private Integer minScore;
    private Long minRank;
    private Long majorId;
    private String majorName;
    private String category;
    private String description;

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
    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

package com.academic.planning.mapper.row;

public class RecommendationCandidateRow {
    private Long schoolId;
    private String schoolName;
    private String schoolProvince;
    private String city;
    private String level;
    private String description;
    private String majorName;
    private Integer minScore;
    private Long minRank;

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getSchoolProvince() { return schoolProvince; }
    public void setSchoolProvince(String schoolProvince) { this.schoolProvince = schoolProvince; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public Integer getMinScore() { return minScore; }
    public void setMinScore(Integer minScore) { this.minScore = minScore; }
    public Long getMinRank() { return minRank; }
    public void setMinRank(Long minRank) { this.minRank = minRank; }
}

package com.academic.planning.mapper.row;

public class AgentSchoolRow {
    private Long id;
    private String name;
    private String province;
    private String city;
    private String level;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}

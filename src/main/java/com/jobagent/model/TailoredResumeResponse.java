package com.jobagent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TailoredResumeResponse {
    private String tailoredSummary;
    private Map<String, List<String>> tailoredSkills;
    private List<List<String>> workExperienceBullets;
    private List<List<String>> projectBullets;
}

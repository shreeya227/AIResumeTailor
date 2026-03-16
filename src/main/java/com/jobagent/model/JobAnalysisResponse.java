package com.jobagent.model;

import lombok.Data;

import java.util.List;

@Data
public class JobAnalysisResponse {

    private List<String> requiredSkills;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private int matchScore;
}

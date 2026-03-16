package com.jobagent.model;

import lombok.Data;

import java.util.List;

@Data
public class ResumeDataModel {

    private String summary;
    private List<String> skills;
    private List<Experience> experience;
    private List<Project> projects;
    private List<Education> education;
    private List<Award> awards;
}

package com.jobagent.model;

import lombok.Data;

import java.util.List;

@Data
public class Project {

    private String name;
    private String type;
    private String link;
    private List<String> technologies;
    private List<String> bullets;
}

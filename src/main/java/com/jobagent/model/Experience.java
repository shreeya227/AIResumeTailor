package com.jobagent.model;

import lombok.Data;

import java.util.List;

@Data
public class Experience {

    private String company;
    private String role;
    private String location;
    private String start;
    private String end;
    private List<String> bullets;
}

package com.jobagent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.model.ResumeDataModel;
import org.springframework.stereotype.Service;


import java.io.InputStream;

@Service
public class ResumeService {

    public ResumeDataModel loadResume(){

        try{

            ObjectMapper mapper= new ObjectMapper();
            InputStream input= getClass().getClassLoader().getResourceAsStream("resume_data.json");

            return mapper.readValue(input, ResumeDataModel.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load resume", e);
        }

    }
}

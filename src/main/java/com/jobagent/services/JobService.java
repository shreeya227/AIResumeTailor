package com.jobagent.services;

import com.jobagent.model.JobAnalysisResponse;
import com.jobagent.model.JobRequest;
import com.jobagent.model.ResumeDataModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final LLMService llmService;
    private final ResumeService resumeService;

    public JobService(LLMService llmService, ResumeService resumeService) {
        this.llmService = llmService;
        this.resumeService = resumeService;
    }

    public JobAnalysisResponse analyzeJob(JobRequest request){

        JobAnalysisResponse response= new JobAnalysisResponse();

        List<String> skills = llmService.extractSkills(request.getDescription());

        response.setRequiredSkills(skills);


        List<String> resumeSkills= resumeService.loadResume().getSkills();

        List<String> matchedSkills= skills.stream().filter(resumeSkills::contains).collect(Collectors.toList());

        List<String> missingSkills =
                skills.stream()
                        .filter(skill -> !resumeSkills.contains(skill))
                        .collect(Collectors.toList());

        response.setMatchedSkills(matchedSkills);
        response.setMissingSkills(missingSkills);

        int score =
                (matchedSkills.size() * 100) / skills.size();

        response.setMatchScore(score);

        return response;



    }
}

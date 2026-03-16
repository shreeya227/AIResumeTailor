package com.jobagent.controller;

import com.jobagent.model.JobAnalysisResponse;
import com.jobagent.model.JobRequest;
import com.jobagent.model.ResumeDataModel;
import com.jobagent.model.TailoredResumeResponse;
import com.jobagent.services.JobService;
import com.jobagent.services.ResumeDownloadGenerator;
import com.jobagent.services.ResumeService;
import com.jobagent.services.ResumeTailorService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    private final JobService jobService;
    private final ResumeService resumeService;
    private final ResumeTailorService resumeTailorService;
    private final ResumeDownloadGenerator resumeDownloadGenerator;

    public JobController(JobService jobService, ResumeService resumeService, ResumeTailorService resumeTailorService, ResumeDownloadGenerator resumeDownloadGenerator){
        this.jobService= jobService;
        this.resumeService= resumeService;
        this.resumeTailorService= resumeTailorService;
        this.resumeDownloadGenerator= resumeDownloadGenerator;
    }

    @PostMapping("/analyze")
    public JobAnalysisResponse analyzeJob(@RequestBody JobRequest request) {
        return jobService.analyzeJob(request);
    }


    @PostMapping("/tailor-resume")
    public TailoredResumeResponse tailorResume(@RequestBody JobRequest request){

        ResumeDataModel resume = resumeService.loadResume();

        return resumeTailorService.tailorResume(
                request.getDescription(),
                resume
        );
    }




    @PostMapping("/generate-resume")
    public ResponseEntity<org.springframework.core.io.Resource> generateResume(@RequestBody JobRequest request) {

        ResumeDataModel resume = resumeService.loadResume();

        System.out.println("Starting tailoring");
        TailoredResumeResponse tailored =
                resumeTailorService.tailorResume(
                        request.getDescription(),
                        resume
                );
        System.out.println("Finished tailoring");

        System.out.println("Starting tex generation");
        String texFile =
                resumeDownloadGenerator.generateResume(tailored, request);
        System.out.println("Finished tex generation");

        System.out.println("Starting pdf compile");
        resumeDownloadGenerator.compileLatex(texFile);
        System.out.println("Finished pdf compile");


        String pdfFile = texFile.replace(".tex", ".pdf");

        File pdf = new File(pdfFile);



        Resource resource = new FileSystemResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + pdf.getName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}

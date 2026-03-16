package com.jobagent.services;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ResumeGeneratorService {

    public String generateResume(String summary, String bullets){

        try{

            String template =
                    Files.readString(
                            Path.of("src/main/resources/resume_template.tex")
                    );

            template = template.replace("{{SUMMARY}}", summary);
            template = template.replace("{{BULLETS}}", bullets);

            String fileName = "tailored_resume.tex";

            Files.writeString(
                    Path.of("generated/" + fileName),
                    template
            );

            return fileName;

        }catch(Exception e){

            throw new RuntimeException("Resume generation failed", e);

        }
    }
}
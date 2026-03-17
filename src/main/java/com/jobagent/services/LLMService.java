package com.jobagent.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class LLMService {

    private static final Set<String> BRANDED_AI_TOOLS = Set.of(
            "chatgpt",
            "github copilot",
            "copilot",
            "claude",
            "gemini"
    );

    @Value("${gemini.api.key}")
    private String apiKey;


    public List<String> extractSkills(String jobDescription) {


        try {

            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            String prompt = """
                    You are an expert technical recruiter and ATS keyword extractor.

                    Task:
                    Extract ONLY concrete, resume-searchable technical skills from the job description.

                    Include only:
                    - programming languages
                    - frameworks and libraries
                    - cloud platforms and infrastructure tools
                    - databases and storage technologies
                    - messaging and streaming technologies
                    - APIs, protocols, and architectural technologies
                    - developer tools and DevOps technologies
                    - ML/AI frameworks or platforms when explicitly mentioned

                    Exclude:
                    - soft skills
                    - responsibilities and job duties
                    - vague concepts such as scalability, ownership, leadership, collaboration, quality, solution design, testing plans, end-to-end systems design
                    - degrees, certifications, years of experience
                    - domain nouns that are not specific technical skills

                    Normalization rules:
                    - deduplicate skills
                    - keep the most standard resume-friendly name
                    - prefer concise skill names like "Spring Boot", "REST APIs", "Kafka", "AWS"
                    - do not invent skills not explicitly supported by the job description
                    - if the job description says generic phrases like "AI tools" or "AI productivity tools", keep them generic and do NOT infer branded products such as ChatGPT, GitHub Copilot, Claude, or Gemini unless the job description explicitly names them
                    - do not infer vendor or product names from broad categories

                    Return RAW JSON ONLY.
                    Do not use markdown fences.
                    Do not add commentary.

                    Output format:
                    {
                      "skills": ["skill1", "skill2", "skill3"]
                    }

                    Job description:
                    """ + jobDescription;



            Map<String,Object> bodyMap = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            String body = mapper.writeValueAsString(bodyMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity =
                    new HttpEntity<>(body, headers);

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                            + apiKey;

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);


            JsonNode root = mapper.readTree(response.getBody());

            String text =
                    root.get("candidates")
                            .get(0)
                            .get("content")
                            .get("parts")
                            .get(0)
                            .get("text")
                            .asText();

            String cleaned = text
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode json = mapper.readTree(cleaned);


            List<String> skills = new ArrayList<>();

            json.get("skills").forEach(
                    skill -> {
                        String extractedSkill = skill.asText().trim();
                        if (extractedSkill.isBlank()) {
                            return;
                        }

                        if (shouldKeepExtractedSkill(extractedSkill, jobDescription)) {
                            skills.add(extractedSkill);
                        }
                    }
            );

            return skills;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gemini skill extraction failed" + e.getMessage());

        }
    }

    private boolean shouldKeepExtractedSkill(String extractedSkill, String jobDescription) {
        String normalizedSkill = extractedSkill.toLowerCase();
        String lowerJobDescription = jobDescription == null ? "" : jobDescription.toLowerCase();

        if (BRANDED_AI_TOOLS.contains(normalizedSkill) && !lowerJobDescription.contains(normalizedSkill)) {
            return false;
        }

        return true;
    }
}





package com.jobagent.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.model.Experience;
import com.jobagent.model.Project;
import com.jobagent.model.ResumeDataModel;
import com.jobagent.model.TailoredResumeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeTailorService {

    private static final List<String> SKILL_CATEGORIES = List.of(
            "Languages",
            "Frameworks",
            "Cloud/DevOps",
            "Databases",
            "Architecture/APIs",
            "AI/ML",
            "Tools/Practices"
    );

    private static final List<String> BANNED_INFERRED_PHRASES = List.of(
            "iam security",
            "iam security features",
            "data protection",
            "public cloud",
            "public clouds",
            "ai optimization",
            "applying ml principles",
            "demonstrating strong analytical",
            "advanced data structures and algorithms"
    );

    private static final List<String> AUTH_SECURITY_EVIDENCE_TERMS = List.of(
            "identity",
            "authentication",
            "authorization",
            "session management",
            "jwt",
            "rbac"
    );

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\b\\d+[\\d,\\.]*\\+?%?|\\b\\d+\\s*[xX×]\\s*\\d+\\b|\\bR²\\b");

    @Value("${gemini.api.key}")
    private String apiKey;

    private final LLMService llmService;

    public ResumeTailorService(LLMService llmService) {
        this.llmService = llmService;
    }

    public TailoredResumeResponse tailorResume(String jobDescription, ResumeDataModel resume) {

        try {

            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            String prompt = """
You are an expert technical recruiter and resume writer.

Task:
Tailor the resume content to the job description while staying fully grounded in the provided resume data.

Primary goals:
- Maximize ATS keyword alignment
- Improve relevance and clarity
- Preserve truthfulness
- Make minimal necessary edits rather than rewriting aggressively
- Prefer compact, ATS-friendly phrasing with concrete technologies and measurable impact

Non-negotiable rules:
- Do NOT invent new experience, technologies, achievements, titles, or responsibilities
- Do NOT move accomplishments from one role/project to another
- Do NOT merge bullets across roles or across projects
- Do NOT add tools or frameworks to a role unless they are already supported by that role's original bullets
- Do NOT add architecture, security, compliance, IAM, public cloud, data management, analytics, or algorithm claims unless they are explicitly supported by the original bullet
- You MAY use grounded security wording such as "secure", "security", "authentication", "authorization", or "application security" when the original bullet already contains direct evidence like identity, authentication, authorization, session management, JWT, or RBAC
- Keep each role and project in the exact same order as provided
- Keep approximately the same number of bullets for each role and project
- Keep quantified results only when already present in the source data
- Preserve all existing numbers, metrics, scale indicators, and quantified outcomes whenever they appear in the source
- Return raw JSON only, with no markdown fences and no commentary

Summary rules:
- Write a concise 2-3 sentence summary
- Use only claims supported by the provided resume
- Emphasize technologies and experience that match the job description
- Do not overstate seniority or expertise

Work experience rules:
- Tailor each role independently
- Rewrite bullets only for that specific role
- Prefer minimal edits to the original bullets
- Prioritize exact or close job-description keywords only when they are supported by that role's original content
- Preserve the original meaning and evidence of each bullet
- Preserve specific technologies, frameworks, languages, platforms, datasets, protocols, and stack names whenever they appear in the original bullet
- Prefer exact original tool names over generic substitutions like "modern web technologies", "backend systems", "ML-driven", or "cloud tools"
- Do not remove concrete stack details such as React, TypeScript, Node.js, Express, MongoDB, JWT, RBAC, Spring/Grails, or PostgreSQL when they already exist in the source bullet unless the edit is strictly necessary for clarity
- Do not replace a specific architecture or implementation detail with a broader abstract phrase
- Avoid interpretive phrases like "demonstrating expertise", "showcasing", "reflecting strong", or similar evaluative commentary
- Do not add inferred phrases such as "IAM security", "data protection and management", "public clouds", "advanced data structures and algorithms", or similar unsupported abstractions
- Supported security emphasis is allowed only when directly grounded in source terms like authentication, authorization, identity, session management, JWT, or RBAC
- If a keyword cannot be inserted naturally without changing the factual meaning, leave it out
- When a source bullet contains technologies, name them explicitly in the rewritten bullet instead of replacing them with generic phrases
- Prefer patterns like "Designed backend services using Java and Python" over vague phrasing like "Designed backend services"
- Keep numbers, percentages, latency improvements, dataset sizes, user counts, and scale indicators in the rewritten bullet whenever they exist in the source
- If the original bullet has measurable impact, the rewritten bullet should also contain that measurable impact
- Do not add new numbers or performance claims that are not present in the original bullet

Project rules:
- Tailor each project independently
- Keep bullets truthful and specific to that project only
- Emphasize technologies and outcomes relevant to the job description
- Preserve project-specific domain details, datasets, model names, metrics, and technical methods whenever they appear in the source
- Do not generalize concrete technical content into vague ML or AI language if the original bullet is more specific
- Do not add business, platform, or systems keywords that are not explicitly grounded in the original project bullet
- Preserve model names, dataset names, image sizes, metrics, and optimization details when they appear in the source

Decision rubric for emphasis:
1. Exact keyword match with the job description and supported by the same role/project
2. Close equivalent already present in the same role/project
3. Strong adjacent technology already demonstrated in that same role/project
If none apply, keep the original wording as close as possible.

Style rules:
- Keep bullets concrete, specific, and evidence-based
- Prefer original wording when it is already strong
- Do not add resume-writer commentary or evaluation language
- Do not add generic phrases that reduce specificity
- Keep bullets concise and ATS-friendly
- Start bullets with strong action verbs
- Prefer one compact sentence per bullet
- Include concrete technologies and measurable outcomes whenever available in the source

Final self-check before returning JSON:
- Every rewritten bullet must remain fully supported by its original source bullet
- No unsupported responsibility, domain, security, compliance, cloud, analytics, or algorithm scope may be introduced
- Remove any phrase that feels inferred rather than explicitly evidenced
- Verify that bullets with original numbers still contain those numbers
- Verify that bullets with original technologies still name those technologies explicitly

Return VALID JSON ONLY in this format:
{
  "tailoredSummary": "...",
  "workExperienceBullets": [
    ["bullet1", "bullet2", "bullet3", "bullet4"],
    ["bullet1", "bullet2", "bullet3", "bullet4"],
    ["bullet1", "bullet2", "bullet3", "bullet4"],
    ["bullet1", "bullet2", "bullet3", "bullet4"]
  ],
  "projectBullets": [
    ["bullet1", "bullet2", "bullet3"],
    ["bullet1", "bullet2", "bullet3"]
  ]
}

Job Description:
""" + jobDescription + """

Resume Data:
""" + mapper.writeValueAsString(resume);



            prompt = prompt.replace("\"", "\\\"");

            String body = """
{
 "contents":[
   {
     "parts":[
       {
         "text":"%s"
       }
     ]
   }
 ]
}
""".formatted(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

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

            text = extractJsonPayload(text);

            System.out.println("Tailor raw response:");
            System.out.println(text);


            JsonNode tailoredJson = readTailoredJson(mapper, text);
            List<String> extractedJobSkills = llmService.extractSkills(jobDescription);

            TailoredResumeResponse tailoredResume = new TailoredResumeResponse();
            tailoredResume.setTailoredSummary(tailoredJson.path("tailoredSummary").asText(""));
            tailoredResume.setTailoredSkills(
                    buildTailoredSkills(resume.getSkills(), extractedJobSkills)
            );
            tailoredResume.setWorkExperienceBullets(
                    validateBulletGroups(
                            normalizeBulletGroups(
                                    tailoredJson.get("workExperienceBullets"),
                                    extractExpectedBulletCounts(resume.getExperience()),
                                    4
                            ),
                            extractOriginalBulletsFromExperience(resume.getExperience()),
                            extractOriginalTechnologiesFromExperience(resume.getExperience())
                    )
            );
            tailoredResume.setProjectBullets(
                    validateBulletGroups(
                            normalizeBulletGroups(
                                    tailoredJson.get("projectBullets"),
                                    extractExpectedBulletCounts(resume.getProjects()),
                                    2
                            ),
                            extractOriginalBulletsFromProjects(resume.getProjects()),
                            extractOriginalTechnologiesFromProjects(resume.getProjects())
                    )
            );

            return tailoredResume;

        } catch (Exception e) {
            throw new RuntimeException("Resume tailoring failed", e);

        }
    }

    private String extractJsonPayload(String text) {

        String cleaned = text.replace("```json", "")
                .replace("```", "")
                .trim();

        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        return cleaned;
    }

    private JsonNode readTailoredJson(ObjectMapper mapper, String text) throws Exception {

        try {
            return mapper.readTree(text);
        } catch (Exception firstParseError) {
            String repaired = repairJson(text);
            return mapper.readTree(repaired);
        }
    }

    private String repairJson(String text) {

        String repaired = text.replaceAll(",\\s*([}\\]])", "$1");
        StringBuilder builder = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < repaired.length(); i++) {
            char current = repaired.charAt(i);

            if (current == '{' || current == '[') {
                stack.push(current);
                builder.append(current);
                continue;
            }

            if (current == '}' || current == ']') {
                while (!stack.isEmpty() && !matches(stack.peek(), current)) {
                    builder.append(stack.peek() == '[' ? ']' : '}');
                    stack.pop();
                }

                if (!stack.isEmpty()) {
                    stack.pop();
                }

                builder.append(current);
                continue;
            }

            builder.append(current);
        }

        while (!stack.isEmpty()) {
            builder.append(stack.pop() == '[' ? ']' : '}');
        }

        return builder.toString();
    }

    private boolean matches(char open, char close) {
        return (open == '{' && close == '}') || (open == '[' && close == ']');
    }

    private Map<String, List<String>> buildTailoredSkills(List<String> resumeSkills, List<String> extractedJobSkills) {

        Map<String, List<String>> categorizedSkills = emptySkillCategories();

        if (resumeSkills == null || resumeSkills.isEmpty()) {
            return categorizedSkills;
        }

        Set<String> prioritizedSkills = new LinkedHashSet<>();

        if (extractedJobSkills != null) {
            for (String extractedSkill : extractedJobSkills) {
                String matchedResumeSkill = findMatchingResumeSkill(extractedSkill, resumeSkills);

                if (matchedResumeSkill != null) {
                    prioritizedSkills.add(matchedResumeSkill);
                }
            }
        }

        List<String> orderedSkills = new ArrayList<>(prioritizedSkills);

        for (String resumeSkill : resumeSkills) {
            if (!prioritizedSkills.contains(resumeSkill)) {
                orderedSkills.add(resumeSkill);
            }
        }

        for (String skill : orderedSkills) {
            categorizedSkills.get(categorizeSkill(skill)).add(skill);
        }

        return categorizedSkills;
    }

    private List<List<String>> normalizeBulletGroups(
            JsonNode bulletNode,
            List<Integer> expectedBulletCounts,
            int expectedGroups
    ) {

        if (bulletNode == null || !bulletNode.isArray()) {
            return emptyGroups(expectedGroups);
        }

        List<List<String>> groupedBullets = new ArrayList<>();

        if (!bulletNode.isEmpty() && bulletNode.get(0).isArray()) {
            for (int i = 0; i < expectedGroups; i++) {
                JsonNode groupNode = i < bulletNode.size() ? bulletNode.get(i) : null;
                groupedBullets.add(readBulletGroup(groupNode));
            }

            return groupedBullets;
        }

        List<String> flatBullets = readBulletGroup(bulletNode);
        int cursor = 0;

        for (int i = 0; i < expectedGroups; i++) {
            int requestedCount =
                    i < expectedBulletCounts.size() ? expectedBulletCounts.get(i) : 0;
            int remainingGroups = expectedGroups - i;
            int remainingBullets = flatBullets.size() - cursor;
            int groupSize = Math.min(requestedCount, Math.max(remainingBullets - (remainingGroups - 1), 0));

            if (groupSize <= 0 && remainingBullets > 0) {
                groupSize = Math.max(1, remainingBullets / remainingGroups);
            }

            List<String> group = new ArrayList<>();

            for (int j = 0; j < groupSize && cursor < flatBullets.size(); j++) {
                group.add(flatBullets.get(cursor++));
            }

            groupedBullets.add(group);
        }

        return groupedBullets;
    }

    private List<String> readBulletGroup(JsonNode groupNode) {

        if (groupNode == null || !groupNode.isArray()) {
            return Collections.emptyList();
        }

        List<String> bullets = new ArrayList<>();

        for (JsonNode bullet : groupNode) {
            bullets.add(bullet.asText());
        }

        return bullets;
    }

    private List<List<String>> validateBulletGroups(
            List<List<String>> tailoredGroups,
            List<List<String>> originalGroups,
            List<Set<String>> requiredTechnologiesByGroup
    ) {

        List<List<String>> validatedGroups = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < originalGroups.size(); groupIndex++) {
            List<String> originalGroup = originalGroups.get(groupIndex);
            List<String> tailoredGroup =
                    tailoredGroups != null && groupIndex < tailoredGroups.size()
                            ? tailoredGroups.get(groupIndex)
                            : Collections.emptyList();
            Set<String> requiredTechnologies =
                    requiredTechnologiesByGroup != null && groupIndex < requiredTechnologiesByGroup.size()
                            ? requiredTechnologiesByGroup.get(groupIndex)
                            : Collections.emptySet();

            List<String> validatedGroup = new ArrayList<>();

            for (int bulletIndex = 0; bulletIndex < originalGroup.size(); bulletIndex++) {
                String originalBullet = originalGroup.get(bulletIndex);
                String tailoredBullet =
                        bulletIndex < tailoredGroup.size() ? tailoredGroup.get(bulletIndex) : originalBullet;

                validatedGroup.add(
                        isSafeTailoredBullet(tailoredBullet, originalBullet, requiredTechnologies)
                                ? tailoredBullet
                                : originalBullet
                );
            }

            validatedGroups.add(validatedGroup);
        }

        return validatedGroups;
    }

    private List<Integer> extractExpectedBulletCounts(List<?> items) {

        if (items == null) {
            return Collections.emptyList();
        }

        List<Integer> counts = new ArrayList<>();

        for (Object item : items) {
            if (item instanceof Experience experience) {
                counts.add(experience.getBullets() != null ? experience.getBullets().size() : 0);
            } else if (item instanceof Project project) {
                counts.add(project.getBullets() != null ? project.getBullets().size() : 0);
            }
        }

        return counts;
    }

    private List<List<String>> extractOriginalBulletsFromExperience(List<Experience> experiences) {

        if (experiences == null) {
            return Collections.emptyList();
        }

        List<List<String>> groups = new ArrayList<>();

        for (Experience experience : experiences) {
            groups.add(experience.getBullets() != null ? experience.getBullets() : Collections.emptyList());
        }

        return groups;
    }

    private List<List<String>> extractOriginalBulletsFromProjects(List<Project> projects) {

        if (projects == null) {
            return Collections.emptyList();
        }

        List<List<String>> groups = new ArrayList<>();

        for (Project project : projects) {
            groups.add(project.getBullets() != null ? project.getBullets() : Collections.emptyList());
        }

        return groups;
    }

    private List<Set<String>> extractOriginalTechnologiesFromExperience(List<Experience> experiences) {

        if (experiences == null) {
            return Collections.emptyList();
        }

        List<Set<String>> technologiesByGroup = new ArrayList<>();

        for (Experience experience : experiences) {
            technologiesByGroup.add(extractTechnologyTokens(experience.getBullets()));
        }

        return technologiesByGroup;
    }

    private List<Set<String>> extractOriginalTechnologiesFromProjects(List<Project> projects) {

        if (projects == null) {
            return Collections.emptyList();
        }

        List<Set<String>> technologiesByGroup = new ArrayList<>();

        for (Project project : projects) {
            List<String> projectText = new ArrayList<>();

            if (project.getTechnologies() != null) {
                projectText.addAll(project.getTechnologies());
            }

            if (project.getBullets() != null) {
                projectText.addAll(project.getBullets());
            }

            technologiesByGroup.add(extractTechnologyTokens(projectText));
        }

        return technologiesByGroup;
    }

    private List<List<String>> emptyGroups(int expectedGroups) {

        List<List<String>> groups = new ArrayList<>();

        for (int i = 0; i < expectedGroups; i++) {
            groups.add(Collections.emptyList());
        }

        return groups;
    }

    private boolean isSafeTailoredBullet(
            String tailoredBullet,
            String originalBullet,
            Set<String> requiredTechnologies
    ) {

        if (tailoredBullet == null || tailoredBullet.isBlank()) {
            return false;
        }

        String tailoredLower = tailoredBullet.toLowerCase(Locale.ROOT);
        String originalLower = originalBullet.toLowerCase(Locale.ROOT);
        boolean hasAuthSecurityEvidence = hasAnyTerm(originalLower, AUTH_SECURITY_EVIDENCE_TERMS);

        for (String bannedPhrase : BANNED_INFERRED_PHRASES) {
            if (tailoredLower.contains(bannedPhrase) && !originalLower.contains(bannedPhrase)) {
                return false;
            }
        }

        List<String> originalNumbers = extractNumbers(originalBullet);

        for (String number : originalNumbers) {
            if (!tailoredBullet.contains(number)) {
                return false;
            }
        }

        for (String technology : requiredTechnologies) {
            if (originalLower.contains(technology) && !tailoredLower.contains(technology)) {
                return false;
            }
        }

        if ((tailoredLower.contains("iam") || tailoredLower.contains("identity resilience"))
                && !originalLower.contains("iam")
                && !originalLower.contains("identity resilience")) {
            return false;
        }

        if ((tailoredLower.contains("secure ") || tailoredLower.contains(" security"))
                && !hasAuthSecurityEvidence
                && !originalLower.contains("secure")
                && !originalLower.contains("security")) {
            return false;
        }

        return true;
    }

    private List<String> extractNumbers(String text) {

        List<String> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        return numbers;
    }

    private Set<String> extractTechnologyTokens(List<String> texts) {

        Set<String> tokens = new LinkedHashSet<>();

        if (texts == null) {
            return tokens;
        }

        String[] knownTerms = {
                "python", "java", "typescript", "javascript", "react", "vue.js", "node.js",
                "fastapi", "express", "mongodb", "postgresql", "mysql", "redis", "docker",
                "aws", "kubernetes", "github actions", "jenkins", "terraform", "jwt", "rbac",
                "spring", "grails", "pytorch", "keras", "scikit-learn", "cnn", "efficientnet",
                "vgg16", "adamw", "bcewithlogitsloss", "microservices", "restful", "rest"
        };

        for (String text : texts) {
            if (text == null) {
                continue;
            }

            String lower = text.toLowerCase(Locale.ROOT);

            for (String term : knownTerms) {
                if (lower.contains(term)) {
                    tokens.add(term);
                }
            }
        }

        return tokens;
    }

    private boolean hasAnyTerm(String text, List<String> terms) {

        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, List<String>> emptySkillCategories() {

        Map<String, List<String>> categories = new LinkedHashMap<>();

        for (String category : SKILL_CATEGORIES) {
            categories.put(category, new ArrayList<>());
        }

        return categories;
    }

    private String categorizeSkill(String skill) {

        if (skill == null) {
            return "Tools/Practices";
        }

        if (Arrays.asList("Python", "Java", "TypeScript", "JavaScript").contains(skill)) {
            return "Languages";
        }

        if (Arrays.asList("React", "Vue.js", "Node.js", "FastAPI", "Express").contains(skill)) {
            return "Frameworks";
        }

        if (Arrays.asList("AWS", "Docker", "Kubernetes", "GitHub Actions", "Jenkins", "Terraform").contains(skill)) {
            return "Cloud/DevOps";
        }

        if (Arrays.asList("MySQL", "PostgreSQL", "Redis", "MongoDB").contains(skill)) {
            return "Databases";
        }

        if (Arrays.asList("Microservices", "Event-Driven Systems", "API Design", "REST APIs", "GraphQL", "Web Services").contains(skill)) {
            return "Architecture/APIs";
        }

        if (Arrays.asList("PyTorch", "Keras", "Scikit-learn", "Deep Learning", "Neural Networks", "Computer Vision", "Large Language Models", "GenAI").contains(skill)) {
            return "AI/ML";
        }

        return "Tools/Practices";
    }

    private String findMatchingResumeSkill(String extractedSkill, List<String> resumeSkills) {

        if (extractedSkill == null || extractedSkill.isBlank()) {
            return null;
        }

        String normalizedExtractedSkill = normalizeSkill(extractedSkill);

        for (String resumeSkill : resumeSkills) {
            String normalizedResumeSkill = normalizeSkill(resumeSkill);

            if (normalizedResumeSkill.equals(normalizedExtractedSkill)
                    || normalizedResumeSkill.contains(normalizedExtractedSkill)
                    || normalizedExtractedSkill.contains(normalizedResumeSkill)) {
                return resumeSkill;
            }
        }

        return null;
    }

    private String normalizeSkill(String skill) {

        return skill.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
    }
}

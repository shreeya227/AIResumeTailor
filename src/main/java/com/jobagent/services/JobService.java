package com.jobagent.services;

import com.jobagent.model.JobAnalysisResponse;
import com.jobagent.model.JobRequest;
import com.jobagent.model.Project;
import com.jobagent.model.ResumeDataModel;
import com.jobagent.model.Experience;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class JobService {

    private static final Set<String> BRANDED_AI_TOOLS = Set.of(
            "chatgpt",
            "github copilot",
            "copilot",
            "claude",
            "gemini"
    );

    private record SkillConcept(
            String canonicalName,
            List<String> aliases,
            List<String> supportingSkills,
            List<String> evidencePatterns
    ) {
    }

    private static final Set<String> NON_SKILL_TERMS = Set.of(
            "scalability",
            "solution design",
            "backend technical solution design",
            "end to end systems design",
            "end-to-end systems design",
            "testing plans",
            "analytical skills",
            "problem solving",
            "problem-solving",
            "ownership",
            "leadership",
            "collaboration"
    );

    private static final List<SkillConcept> OPERATIONAL_CONCEPTS = List.of(
            new SkillConcept(
                    "code reviews",
                    List.of("code review", "code reviews"),
                    List.of(),
                    List.of("code review", "code reviews")
            ),
            new SkillConcept(
                    "production support",
                    List.of("production support", "production systems", "production issues"),
                    List.of(),
                    List.of("production support", "production systems", "production issues", "stable releases")
            ),
            new SkillConcept(
                    "on call",
                    List.of("on call", "on-call", "on call rotation", "on-call rotation"),
                    List.of(),
                    List.of("on-call", "on call", "production support")
            ),
            new SkillConcept(
                    "troubleshooting",
                    List.of("troubleshooting", "troubleshoot", "debugging"),
                    List.of(),
                    List.of("troubleshooting", "troubleshoot", "debugging", "error tracking")
            ),
            new SkillConcept(
                    "legacy modernization",
                    List.of(
                            "legacy modernization",
                            "modernization",
                            "modernize legacy systems",
                            "upgrading legacy dependencies",
                            "legacy systems"
                    ),
                    List.of(),
                    List.of("legacy", "refactored legacy", "refactored legacy components", "modernization")
            ),
            new SkillConcept(
                    "test coverage",
                    List.of("test coverage", "unit tests", "integration tests", "end to end tests", "end-to-end tests"),
                    List.of(),
                    List.of("automated tests", "unit testing", "integration testing", "testing")
            )
    );

    private static final Map<String, String> SKILL_SYNONYMS = Map.ofEntries(
            Map.entry("rest api", "rest apis"),
            Map.entry("restful api", "rest apis"),
            Map.entry("restful apis", "rest apis"),
            Map.entry("express.js", "express"),
            Map.entry("express js", "express"),
            Map.entry("node", "node.js"),
            Map.entry("graphql api", "graphql"),
            Map.entry("graphql apis", "graphql"),
            Map.entry("llm", "large language models"),
            Map.entry("llms", "large language models"),
            Map.entry("k8s", "kubernetes"),
            Map.entry("golang", "go"),
            Map.entry("jwt token", "jwt"),
            Map.entry("json web token", "jwt"),
            Map.entry("oauth2", "oauth 2.0"),
            Map.entry("oauth", "oauth 2.0")
    );

    private static final List<SkillConcept> SKILL_TAXONOMY = List.of(
            new SkillConcept(
                    "ci cd",
                    List.of(
                            "ci cd",
                            "ci/cd",
                            "ci cd pipeline",
                            "ci/cd pipeline",
                            "ci cd pipelines",
                            "ci/cd pipelines",
                            "continuous integration",
                            "continuous delivery",
                            "continuous deployment"
                    ),
                    List.of("github actions", "jenkins", "terraform"),
                    List.of(
                            "ci/cd",
                            "ci cd",
                            "github actions",
                            "jenkins",
                            "deployment pipeline",
                            "build pipeline",
                            "release pipeline",
                            "continuous integration",
                            "continuous delivery",
                            "continuous deployment"
                    )
            ),
            new SkillConcept(
                    "infrastructure as code",
                    List.of("infrastructure as code", "iac"),
                    List.of("terraform"),
                    List.of("terraform", "iac")
            ),
            new SkillConcept(
                    "sql",
                    List.of("sql"),
                    List.of("postgresql", "mysql"),
                    List.of("postgresql", "mysql", "sql")
            ),
            new SkillConcept(
                    "sql database",
                    List.of("sql database"),
                    List.of("postgresql", "mysql"),
                    List.of("postgresql", "mysql")
            ),
            new SkillConcept(
                    "relational database",
                    List.of("relational database", "relational databases"),
                    List.of("postgresql", "mysql"),
                    List.of("postgresql", "mysql")
            ),
            new SkillConcept(
                    "nosql database",
                    List.of("nosql database", "nosql databases"),
                    List.of("mongodb", "redis"),
                    List.of("mongodb", "redis")
            ),
            new SkillConcept(
                    "nosql",
                    List.of("nosql"),
                    List.of("mongodb", "redis"),
                    List.of("mongodb", "redis")
            ),
            new SkillConcept(
                    "database",
                    List.of("database", "databases"),
                    List.of("mongodb", "mysql", "postgresql", "redis"),
                    List.of("postgresql", "mysql", "mongodb", "redis", "schema", "query optimization", "data modeling")
            ),
            new SkillConcept(
                    "frontend framework",
                    List.of("frontend framework", "frontend frameworks"),
                    List.of("react", "vue.js"),
                    List.of("react", "vue")
            ),
            new SkillConcept(
                    "backend framework",
                    List.of("backend framework", "backend frameworks"),
                    List.of("fastapi", "express", "node.js"),
                    List.of("fastapi", "express", "node.js", "framework")
            ),
            new SkillConcept(
                    "cloud platform",
                    List.of("cloud platform", "cloud platforms", "cloud infrastructure"),
                    List.of("aws", "kubernetes", "docker"),
                    List.of("aws", "docker", "kubernetes")
            ),
            new SkillConcept(
                    "devops",
                    List.of("devops", "dev ops"),
                    List.of("docker", "kubernetes", "github actions", "jenkins", "terraform"),
                    List.of(
                            "devops",
                            "github actions",
                            "jenkins",
                            "docker",
                            "kubernetes",
                            "terraform",
                            "deployment",
                            "environment provisioning"
                    )
            ),
            new SkillConcept(
                    "containerization",
                    List.of("containerization", "containers"),
                    List.of("docker", "kubernetes"),
                    List.of("docker", "kubernetes", "container")
            ),
            new SkillConcept(
                    "container orchestration",
                    List.of("container orchestration"),
                    List.of("kubernetes"),
                    List.of("kubernetes")
            ),
            new SkillConcept(
                    "security",
                    List.of("security", "security best practices"),
                    List.of("jwt", "rbac"),
                    List.of(
                            "secure restful api",
                            "secure rest api",
                            "secure authentication",
                            "authentication and authorization",
                            "jwt",
                            "rbac",
                            "identity authentication"
                    )
            ),
            new SkillConcept(
                    "authentication authorization",
                    List.of("authentication authorization"),
                    List.of("jwt", "rbac"),
                    List.of("authentication", "authorization", "jwt", "rbac", "session management")
            ),
            new SkillConcept(
                    "authentication",
                    List.of("authentication"),
                    List.of("jwt", "rbac"),
                    List.of("authentication", "identity", "session management", "jwt", "rbac")
            ),
            new SkillConcept(
                    "authorization",
                    List.of("authorization"),
                    List.of("jwt", "rbac"),
                    List.of("authorization", "rbac", "jwt")
            ),
            new SkillConcept(
                    "monitoring",
                    List.of("monitoring"),
                    List.of("observability"),
                    List.of("monitoring", "observability", "error tracking")
            ),
            new SkillConcept(
                    "logging",
                    List.of("logging"),
                    List.of("observability"),
                    List.of("logging", "structured logging")
            ),
            new SkillConcept(
                    "alerting",
                    List.of("alerting"),
                    List.of("observability"),
                    List.of("error tracking", "observability")
            ),
            new SkillConcept(
                    "distributed systems",
                    List.of("distributed systems", "distributed system"),
                    List.of("microservices", "event-driven systems"),
                    List.of(
                            "distributed systems",
                            "microservices",
                            "event-driven",
                            "distributed experimentation",
                            "high-throughput",
                            "api-driven data workflows"
                    )
            ),
            new SkillConcept(
                    "event-driven architecture",
                    List.of("event-driven architecture"),
                    List.of("event-driven systems"),
                    List.of("event-driven systems", "event-driven")
            ),
            new SkillConcept(
                    "jwt",
                    List.of("jwt", "jwt token", "json web token"),
                    List.of("jwt"),
                    List.of("jwt", "json web token")
            )
    );

    private static final Map<String, String> TAXONOMY_ALIASES = buildTaxonomyAliases();
    private static final Map<String, List<String>> TAXONOMY_SUPPORTING_SKILLS = buildSupportingSkills();
    private static final Map<String, List<String>> TAXONOMY_EVIDENCE_PATTERNS = buildEvidencePatterns();
    private static final Map<String, String> REQUIREMENT_FAMILIES = buildRequirementFamilies();
    private static final Map<String, String> DIRECT_EVIDENCE_ALIASES = buildDirectEvidenceAliases();

    private static final int EXACT_MATCH_WEIGHT = 100;
    private static final int RELATED_MATCH_WEIGHT = 50;

    private final LLMService llmService;
    private final ResumeService resumeService;

    public JobService(LLMService llmService, ResumeService resumeService) {
        this.llmService = llmService;
        this.resumeService = resumeService;
    }

    public JobAnalysisResponse analyzeJob(JobRequest request){

        JobAnalysisResponse response = new JobAnalysisResponse();

        List<String> extractedSkills = llmService.extractSkills(request.getDescription());
        List<String> normalizedRequiredSkills = normalizeRequiredSkills(extractedSkills, request.getDescription());
        Set<String> preferredRequirementFamilies = extractPreferredRequirementFamilies(request.getDescription());
        ResumeDataModel resume = resumeService.loadResume();
        List<String> resumeSkills = resume.getSkills();
        Map<String, String> normalizedResumeSkillMap = buildNormalizedResumeSkillMap(resumeSkills);
        Map<String, String> normalizedResumeEvidenceMap = buildNormalizedResumeEvidenceMap(resume);

        response.setRequiredSkills(normalizedRequiredSkills);

        List<String> matchedSkills = new ArrayList<>();
        List<String> relatedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        Map<String, Integer> familyScoreMap = new LinkedHashMap<>();
        Map<String, Boolean> familyExactMatchMap = new LinkedHashMap<>();

        for (String requiredSkill : normalizedRequiredSkills) {
            String normalizedRequiredSkill = normalizeSkill(requiredSkill);
            String requirementFamily = familyFor(normalizedRequiredSkill);
            int skillWeight;
            boolean exactMatch = false;

            if (normalizedResumeSkillMap.containsKey(normalizedRequiredSkill)) {
                matchedSkills.add(normalizedResumeSkillMap.get(normalizedRequiredSkill));
                skillWeight = EXACT_MATCH_WEIGHT;
                exactMatch = true;
            } else if (normalizedResumeEvidenceMap.containsKey(normalizedRequiredSkill)) {
                relatedSkills.add(requiredSkill + " (" + normalizedResumeEvidenceMap.get(normalizedRequiredSkill) + ")");
                skillWeight = RELATED_MATCH_WEIGHT;
            } else if (hasGroupedMatch(normalizedRequiredSkill, normalizedResumeEvidenceMap)) {
                relatedSkills.add(formatRelatedSkill(requiredSkill, normalizedRequiredSkill, normalizedResumeEvidenceMap));
                skillWeight = RELATED_MATCH_WEIGHT;
            } else {
                missingSkills.add(requiredSkill);
                skillWeight = 0;
            }

            int weightedSkillPoints = applyImportanceWeight(skillWeight, requirementFamily, preferredRequirementFamilies);
            familyScoreMap.merge(requirementFamily, weightedSkillPoints, Math::max);
            familyExactMatchMap.merge(requirementFamily, exactMatch, Boolean::logicalOr);
        }

        response.setMatchedSkills(matchedSkills);
        response.setRelatedSkills(relatedSkills);
        response.setMissingSkills(missingSkills);

        int weightedMatchPoints = familyScoreMap.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        int totalPossiblePoints = familyScoreMap.keySet().stream()
                .mapToInt(family -> applyImportanceWeight(EXACT_MATCH_WEIGHT, family, preferredRequirementFamilies))
                .sum();

        int score = familyScoreMap.isEmpty()
                ? 0
                : (weightedMatchPoints * 100) / totalPossiblePoints;

        int exactCoreFamilyMatches = (int) familyExactMatchMap.entrySet().stream()
                .filter(entry -> !preferredRequirementFamilies.contains(entry.getKey()))
                .filter(Map.Entry::getValue)
                .count();

        response.setMatchScore(capScore(score, exactCoreFamilyMatches));

        return response;
    }

    private List<String> normalizeRequiredSkills(List<String> extractedSkills, String jobDescription) {

        List<String> cleanedSkills = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String skill : extractedSkills == null ? List.<String>of() : extractedSkills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }

            String normalized = normalizeSkill(skill);

            if (normalized.isBlank()
                    || NON_SKILL_TERMS.contains(normalized)
                    || isUnsupportedBrandedAiTool(normalized, jobDescription)) {
                continue;
            }

            String canonical = canonicalizeSkill(skill);
            String canonicalNormalized = normalizeSkill(canonical);

            if (seen.add(canonicalNormalized)) {
                cleanedSkills.add(canonical);
            }
        }

        if (jobDescription != null && !jobDescription.isBlank()) {
            String lowerDescription = jobDescription.toLowerCase(Locale.ROOT);

            for (SkillConcept concept : OPERATIONAL_CONCEPTS) {
                for (String alias : concept.aliases()) {
                    if (lowerDescription.contains(alias) && seen.add(concept.canonicalName())) {
                        cleanedSkills.add(toDisplaySkill(concept.canonicalName()));
                        break;
                    }
                }
            }
        }

        return cleanedSkills;
    }

    private Map<String, String> buildNormalizedResumeSkillMap(List<String> resumeSkills) {

        Map<String, String> normalizedResumeSkillMap = new LinkedHashMap<>();

        if (resumeSkills == null) {
            return normalizedResumeSkillMap;
        }

        for (String resumeSkill : resumeSkills) {
            normalizedResumeSkillMap.putIfAbsent(normalizeSkill(resumeSkill), resumeSkill);
        }

        return normalizedResumeSkillMap;
    }

    private Map<String, String> buildNormalizedResumeEvidenceMap(ResumeDataModel resume) {

        Map<String, String> evidenceMap = buildNormalizedResumeSkillMap(resume.getSkills());

        if (resume.getExperience() != null) {
            for (Experience experience : resume.getExperience()) {
                if (experience.getBullets() != null) {
                    for (String bullet : experience.getBullets()) {
                        addEvidenceText(evidenceMap, bullet, experience.getRole());
                    }
                }
            }
        }

        if (resume.getProjects() != null) {
            for (Project project : resume.getProjects()) {
                if (project.getTechnologies() != null) {
                    for (String technology : project.getTechnologies()) {
                        addEvidenceText(evidenceMap, technology, project.getName());
                    }
                }

                if (project.getBullets() != null) {
                    for (String bullet : project.getBullets()) {
                        addEvidenceText(evidenceMap, bullet, project.getName());
                    }
                }
            }
        }

        // Summary is useful fallback evidence, but it should not override stronger
        // evidence that appears in experience or project content.
        addEvidenceText(evidenceMap, resume.getSummary(), "Summary");

        return evidenceMap;
    }

    private String canonicalizeSkill(String skill) {
        String normalized = normalizeSkill(skill);

        if (SKILL_SYNONYMS.containsKey(normalized)) {
            return toDisplaySkill(SKILL_SYNONYMS.get(normalized));
        }

        if (TAXONOMY_ALIASES.containsKey(normalized)) {
            return toDisplaySkill(TAXONOMY_ALIASES.get(normalized));
        }

        return skill.trim();
    }

    private String normalizeSkill(String skill) {

        String normalized = skill.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9+.#/ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.endsWith("s") && normalized.length() > 3 && !normalized.endsWith(".js")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (SKILL_SYNONYMS.containsKey(normalized)) {
            return SKILL_SYNONYMS.get(normalized);
        }

        return TAXONOMY_ALIASES.getOrDefault(normalized, normalized);
    }

    private String toDisplaySkill(String normalizedSkill) {
        return switch (normalizedSkill) {
            case "rest apis" -> "REST APIs";
            case "node.js" -> "Node.js";
            case "large language models" -> "Large Language Models";
            case "kubernetes" -> "Kubernetes";
            case "go" -> "Go";
            case "sql" -> "SQL";
            case "nosql" -> "NoSQL";
            case "devops" -> "DevOps";
            case "security" -> "Security";
            case "jwt" -> "JWT";
            case "code reviews" -> "Code Reviews";
            case "production support" -> "Production Support";
            case "on call" -> "On-Call";
            case "troubleshooting" -> "Troubleshooting";
            case "legacy modernization" -> "Legacy Modernization";
            case "test coverage" -> "Test Coverage";
            case "nosql database" -> "NoSQL database";
            case "sql database" -> "SQL database";
            case "relational database" -> "Relational database";
            case "frontend framework" -> "Frontend framework";
            case "backend framework" -> "Backend framework";
            case "cloud platform" -> "Cloud platform";
            case "containerization" -> "Containerization";
            case "container orchestration" -> "Container orchestration";
            case "infrastructure as code" -> "Infrastructure as Code";
            case "authentication authorization" -> "Authentication/Authorization";
            case "distributed systems" -> "Distributed Systems";
            case "event-driven architecture" -> "Event-Driven Architecture";
            case "ci cd" -> "CI/CD";
            default -> normalizedSkill;
        };
    }

    private boolean hasGroupedMatch(String normalizedRequiredSkill, Map<String, String> normalizedResumeSkillMap) {

        if (!TAXONOMY_SUPPORTING_SKILLS.containsKey(normalizedRequiredSkill)) {
            return false;
        }

        for (String relatedSkill : TAXONOMY_SUPPORTING_SKILLS.get(normalizedRequiredSkill)) {
            if (normalizedResumeSkillMap.containsKey(relatedSkill)) {
                return true;
            }
        }

        return false;
    }

    private String formatRelatedSkill(
            String requiredSkill,
            String normalizedRequiredSkill,
            Map<String, String> normalizedResumeSkillMap
    ) {

        List<String> supportedSkills = new ArrayList<>();

        for (String relatedSkill : TAXONOMY_SUPPORTING_SKILLS.getOrDefault(normalizedRequiredSkill, List.of())) {
            if (normalizedResumeSkillMap.containsKey(relatedSkill)) {
                supportedSkills.add(normalizedResumeSkillMap.get(relatedSkill));
            }
        }

        if (supportedSkills.isEmpty()) {
            return requiredSkill;
        }

        return requiredSkill + " (" + String.join(", ", supportedSkills) + ")";
    }

    private void addEvidenceText(Map<String, String> evidenceMap, String text, String sourceLabel) {

        if (text == null || text.isBlank()) {
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        String normalizedText = " " + normalizeSkill(text) + " ";

        for (Map.Entry<String, String> aliasEntry : DIRECT_EVIDENCE_ALIASES.entrySet()) {
            if (normalizedText.contains(" " + aliasEntry.getKey() + " ")) {
                evidenceMap.putIfAbsent(aliasEntry.getValue(), sourceLabel);
            }
        }

        for (Map.Entry<String, List<String>> entry : TAXONOMY_EVIDENCE_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (lower.contains(pattern)) {
                    evidenceMap.putIfAbsent(entry.getKey(), sourceLabel);
                    break;
                }
            }
        }
    }

    private static Map<String, String> buildTaxonomyAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();

        for (SkillConcept concept : combinedConcepts()) {
            aliases.putIfAbsent(concept.canonicalName(), concept.canonicalName());
            aliases.putIfAbsent(normalizeStaticAlias(concept.canonicalName()), concept.canonicalName());
            for (String alias : concept.aliases()) {
                aliases.putIfAbsent(alias, concept.canonicalName());
                aliases.putIfAbsent(normalizeStaticAlias(alias), concept.canonicalName());
            }
        }

        return aliases;
    }

    private static Map<String, List<String>> buildSupportingSkills() {
        Map<String, List<String>> supportingSkills = new LinkedHashMap<>();

        for (SkillConcept concept : combinedConcepts()) {
            supportingSkills.put(concept.canonicalName(), concept.supportingSkills());
        }

        return supportingSkills;
    }

    private static Map<String, List<String>> buildEvidencePatterns() {
        Map<String, List<String>> evidencePatterns = new LinkedHashMap<>();

        for (SkillConcept concept : combinedConcepts().stream()
                .sorted(Comparator.comparingInt((SkillConcept concept) -> concept.canonicalName().length()).reversed())
                .toList()) {
            evidencePatterns.put(concept.canonicalName(), concept.evidencePatterns());
        }

        return evidencePatterns;
    }

    private static Map<String, String> buildRequirementFamilies() {
        Map<String, String> families = new LinkedHashMap<>();

        families.put("java", "backend-language");
        families.put("python", "backend-language");
        families.put("go", "backend-language");
        families.put("node.js", "backend-language");

        families.put("postgresql", "relational-data");
        families.put("mysql", "relational-data");
        families.put("sql", "relational-data");
        families.put("sql database", "relational-data");
        families.put("relational database", "relational-data");

        families.put("mongodb", "nosql-data");
        families.put("redis", "nosql-data");
        families.put("nosql", "nosql-data");
        families.put("nosql database", "nosql-data");

        families.put("rest apis", "api-design");
        families.put("graphql", "api-design");

        families.put("express", "backend-framework");
        families.put("django", "backend-framework");
        families.put("fastapi", "backend-framework");
        families.put("spring boot", "backend-framework");

        families.put("aws", "cloud-platform");
        families.put("azure", "cloud-platform");
        families.put("gcp", "cloud-platform");
        families.put("cloud platform", "cloud-platform");

        families.put("microservices", "distributed-architecture");
        families.put("distributed systems", "distributed-architecture");

        families.put("event-driven systems", "event-driven-architecture");
        families.put("event-driven architecture", "event-driven-architecture");

        families.put("terraform", "infrastructure-as-code");
        families.put("infrastructure as code", "infrastructure-as-code");

        families.put("ci cd", "devops-platform");
        families.put("github actions", "devops-platform");
        families.put("jenkins", "devops-platform");
        families.put("devops", "devops-platform");

        families.put("docker", "devops-platform");
        families.put("kubernetes", "devops-platform");
        families.put("containerization", "devops-platform");
        families.put("container orchestration", "devops-platform");

        families.put("jwt", "security-auth");
        families.put("oauth 2.0", "security-auth");
        families.put("authentication", "security-auth");
        families.put("authorization", "security-auth");
        families.put("security", "security-auth");

        families.put("code reviews", "engineering-practices");
        families.put("test coverage", "engineering-practices");
        families.put("production support", "operational-support");
        families.put("on call", "operational-support");
        families.put("troubleshooting", "operational-support");
        families.put("legacy modernization", "modernization");

        families.put("kafka", "messaging-streaming");
        families.put("rabbitmq", "messaging-streaming");
        families.put("stream processing", "messaging-streaming");

        return families;
    }

    private String familyFor(String normalizedSkill) {
        return REQUIREMENT_FAMILIES.getOrDefault(normalizedSkill, normalizedSkill);
    }

    private int familyWeight(String family) {
        return switch (family) {
            case "backend-language",
                 "backend-framework",
                 "relational-data",
                 "nosql-data",
                 "api-design",
                 "cloud-platform",
                 "distributed-architecture" -> EXACT_MATCH_WEIGHT;
            case "devops-platform",
                 "infrastructure-as-code",
                 "security-auth",
                 "event-driven-architecture",
                 "engineering-practices",
                 "operational-support",
                 "modernization" -> 80;
            default -> 60;
        };
    }

    private int applyImportanceWeight(int score, String family, Set<String> preferredRequirementFamilies) {
        if (preferredRequirementFamilies.contains(family)) {
            return (score * 60) / 100;
        }

        return (score * 100) / 100;
    }

    private int capScore(int rawScore, int exactCoreFamilyMatches) {
        if (exactCoreFamilyMatches < 3) {
            return Math.min(rawScore, 82);
        }

        if (exactCoreFamilyMatches < 5) {
            return Math.min(rawScore, 90);
        }

        return Math.min(rawScore, 97);
    }

    private static Map<String, String> buildDirectEvidenceAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();

        Map<String, String> concreteTerms = Map.ofEntries(
                Map.entry("express", "express"),
                Map.entry("graphql", "graphql"),
                Map.entry("django", "django"),
                Map.entry("fastapi", "fastapi"),
                Map.entry("spring boot", "spring boot"),
                Map.entry("java", "java"),
                Map.entry("python", "python"),
                Map.entry("go", "go"),
                Map.entry("node.js", "node.js"),
                Map.entry("postgresql", "postgresql"),
                Map.entry("mysql", "mysql"),
                Map.entry("mongodb", "mongodb"),
                Map.entry("redis", "redis"),
                Map.entry("aws", "aws"),
                Map.entry("azure", "azure"),
                Map.entry("gcp", "gcp"),
                Map.entry("docker", "docker"),
                Map.entry("kubernetes", "kubernetes"),
                Map.entry("terraform", "terraform"),
                Map.entry("github actions", "github actions"),
                Map.entry("jenkins", "jenkins"),
                Map.entry("jwt", "jwt"),
                Map.entry("oauth 2.0", "oauth 2.0"),
                Map.entry("microservices", "microservices"),
                Map.entry("event-driven systems", "event-driven systems"),
                Map.entry("kafka", "kafka"),
                Map.entry("rabbitmq", "rabbitmq"),
                Map.entry("git", "git")
        );

        for (Map.Entry<String, String> entry : concreteTerms.entrySet()) {
            aliases.put(entry.getKey(), entry.getValue());
            aliases.put(normalizeStaticAlias(entry.getKey()), entry.getValue());
        }

        aliases.put("express.js", "express");
        aliases.put("graphql api", "graphql");
        aliases.put("graphql apis", "graphql");

        return aliases;
    }

    private static List<SkillConcept> combinedConcepts() {
        List<SkillConcept> allConcepts = new ArrayList<>(SKILL_TAXONOMY);
        allConcepts.addAll(OPERATIONAL_CONCEPTS);
        return allConcepts;
    }

    private static String normalizeStaticAlias(String skill) {
        String normalized = skill.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9+.#/ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.endsWith("s") && normalized.length() > 3 && !normalized.endsWith(".js")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (SKILL_SYNONYMS.containsKey(normalized)) {
            return SKILL_SYNONYMS.get(normalized);
        }

        return normalized;
    }

    private Set<String> extractPreferredRequirementFamilies(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return Set.of();
        }

        Set<String> preferredFamilies = new LinkedHashSet<>();
        String lowerDescription = jobDescription.toLowerCase(Locale.ROOT);
        boolean inPreferredSection = false;

        for (String rawLine : lowerDescription.split("\\R")) {
            String line = rawLine.trim();

            if (line.isBlank()) {
                continue;
            }

            if (line.contains("nice-to-have") || line.contains("nice to have") || line.contains("preferred")) {
                inPreferredSection = true;
                continue;
            }

            if (line.contains("must-have") || line.contains("must have") || line.contains("required") || line.contains("what you'll bring")) {
                inPreferredSection = false;
            }

            if (!inPreferredSection) {
                continue;
            }

            for (String alias : TAXONOMY_ALIASES.keySet()) {
                if (line.contains(alias)) {
                    preferredFamilies.add(familyFor(TAXONOMY_ALIASES.get(alias)));
                }
            }
        }

        return preferredFamilies;
    }

    private boolean isUnsupportedBrandedAiTool(String normalizedSkill, String jobDescription) {
        if (!BRANDED_AI_TOOLS.contains(normalizedSkill)) {
            return false;
        }

        String lowerJobDescription = jobDescription == null ? "" : jobDescription.toLowerCase(Locale.ROOT);
        return !lowerJobDescription.contains(normalizedSkill);
    }
}

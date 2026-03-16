package com.jobagent.services;

import com.jobagent.model.JobRequest;
import com.jobagent.model.TailoredResumeResponse;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;
import java.util.Map;

@Service
public class ResumeDownloadGenerator {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public String generateResume(TailoredResumeResponse tailoredData, JobRequest request) {

        try {
            String template =
                    Files.readString(Path.of("src/main/resources/resume_template.tex"));

            String summary =
                    sanitizeLatexText(tailoredData.getTailoredSummary());
            String skills =
                    convertSkillsToLatex(tailoredData.getTailoredSkills());

            template = template.replace("{{SUMMARY}}", summary);
            template = template.replace("{{SKILLS}}", skills);
            template = applyGroupedBullets(
                    template,
                    "WORK_BULLETS",
                    tailoredData.getWorkExperienceBullets(),
                    4
            );
            template = applyGroupedBullets(
                    template,
                    "PROJECT_BULLETS",
                    tailoredData.getProjectBullets(),
                    2
            );

            Path outputDir = Path.of("generated");

            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            copyLatexSupportFiles(outputDir);

            String baseFileName = buildBaseFileName(request);
            String texFile = outputDir.resolve(baseFileName + ".tex").toString();

            Files.writeString(Path.of(texFile), template);

            return texFile;

        } catch (Exception e) {
            throw new RuntimeException("Resume generation failed", e);
        }
    }

    public void compileLatex(String texFile) {

        try {
            Path texPath = Path.of(texFile);
            ProcessBuilder pb =
                    new ProcessBuilder(
                            "pdflatex",
                            "-interaction=nonstopmode",
                            texPath.getFileName().toString()
                    );

            pb.directory(texPath.getParent().toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            System.out.println("pdflatex output:");
            System.out.println(output);

            if (exitCode != 0) {
                throw new RuntimeException("PDF generation failed: " + output);
            }

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private String applyGroupedBullets(
            String template,
            String placeholderPrefix,
            List<List<String>> bulletGroups,
            int expectedGroups
    ) {

        for (int i = 0; i < expectedGroups; i++) {
            List<String> bullets =
                    bulletGroups != null && i < bulletGroups.size() ? bulletGroups.get(i) : null;

            template = template.replace(
                    "{{" + placeholderPrefix + "_" + (i + 1) + "}}",
                    convertBulletsToLatex(bullets)
            );
        }

        return template;
    }

    private String convertBulletsToLatex(List<String> bullets) {

        StringBuilder latexBullets = new StringBuilder();

        if (bullets == null) {
            return "";
        }

        for (String bullet : bullets) {
            latexBullets.append("\\item ")
                    .append(sanitizeLatexText(bullet))
                    .append("\n");
        }

        return latexBullets.toString();
    }

    private String convertSkillsToLatex(Map<String, List<String>> skillsByCategory) {

        if (skillsByCategory == null || skillsByCategory.isEmpty()) {
            return "";
        }

        StringBuilder latexSkills = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : skillsByCategory.entrySet()) {
            List<String> skills = entry.getValue();

            if (skills == null || skills.isEmpty()) {
                continue;
            }

            String joinedSkills = String.join(", ", skills);

            latexSkills.append("\\item ")
                    .append("\\textbf{")
                    .append(sanitizeLatexText(entry.getKey()))
                    .append("}: ")
                    .append(sanitizeLatexText(joinedSkills))
                    .append("\n");
        }

        return latexSkills.toString();
    }

    private String buildBaseFileName(JobRequest request) {

        String titlePart = sanitizeFilePart(request.getTitle());
        String companyPart = sanitizeFilePart(request.getCompany());
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);

        StringBuilder fileName = new StringBuilder("resume");

        if (!titlePart.isBlank()) {
            fileName.append("_").append(titlePart);
        }

        if (!companyPart.isBlank()) {
            fileName.append("_").append(companyPart);
        }

        fileName.append("_").append(timestamp);

        return fileName.toString();
    }

    private String sanitizeFilePart(String value) {

        if (value == null) {
            return "";
        }

        String normalized = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return normalized;
    }

    private void copyLatexSupportFiles(Path outputDir) throws Exception {

        try (InputStream classFile = getClass().getClassLoader().getResourceAsStream("resume.cls")) {
            if (classFile == null) {
                throw new RuntimeException("Missing LaTeX class file: resume.cls");
            }

            Files.copy(classFile, outputDir.resolve("resume.cls"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String sanitizeLatexText(String value) {

        if (value == null) {
            return "";
        }

        String sanitized = value
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("^", "\\textasciicircum{}")
                .replace("–", "--")
                .replace("—", "---");

        sanitized = sanitized
                .replace("≈", "$\\sim$")
                .replace("±", "$\\pm$")
                .replace("×10⁻⁵", "$\\times 10^{-5}$")
                .replace("10⁻⁵", "10$^{-5}$")
                .replace("⁻⁵", "$^{-5}$")
                .replace("×", "$\\times$")
                .replace("−", "-")
                .replace("⁻", "-")
                .replace("⁵", "5");

        sanitized = sanitized.replaceAll("~(?=\\d)", "\\$\\\\sim\\$");

        return sanitized;
    }
}

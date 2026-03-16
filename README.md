
# AI Resume Tailor 

An AI-powered resume tailoring platform that analyzes job descriptions and generates customized resumes using LLMs.

The system extracts relevant skills from job postings, rewrites resume content to better match job requirements, and compiles a professionally formatted LaTeX resume into a downloadable PDF.

A Chrome extension allows users to extract job descriptions directly from job listing pages and generate tailored resumes with one click.

```markdown
## Architecture

```mermaid
flowchart LR
    A[Job Posting] --> B[Chrome Extension<br>Extract Description]
    B --> C[Spring Boot Backend]

    C --> D[Gemini API<br>Skill Extraction]
    C --> E[Gemini API<br>Resume Tailoring]

    D --> F[LaTeX Resume Generator]
    E --> F

    F --> G[PDF Resume]
```

## Tech Stack
#### Backend
• Java
• Spring Boot
• Jackson (JSON parsing)
• RestTemplate for API calls
#### AI
• Google Gemini API
#### Resume Generation
• LaTeX
• pdflatex
#### Frontend / Integration
• Chrome Extension
• JavaScript

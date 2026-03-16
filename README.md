An AI-powered resume tailoring platform that analyzes job descriptions and generates customized resumes using LLMs.

The system extracts relevant skills from job postings, rewrites resume content to better match job requirements, and compiles a professionally formatted LaTeX resume into a downloadable PDF.

A Chrome extension allows users to extract job descriptions directly from job listing pages and generate tailored resumes with one click.

## Architecture

```mermaid
flowchart TD
    A[Job Posting Page<br>LinkedIn / Indeed] --> B[Chrome Extension<br>Extract Job Description]
    B --> C[Spring Boot Backend API]

    C --> D[Skill Extraction<br>Gemini API]
    C --> E[Resume Tailoring<br>Gemini API]

    D --> F[Structured Resume Data]
    E --> F

    F --> G[LaTeX Resume Template Engine]
    G --> H[pdflatex Compilation]
    H --> I[Download Tailored Resume PDF]
```

### Tech Stack
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

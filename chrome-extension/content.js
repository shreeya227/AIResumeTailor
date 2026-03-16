function extractJobDescription() {
    const selectors = [
        // LinkedIn variants
        ".jobs-description-content",
        ".jobs-box__html-content",
        ".jobs-description__content",
        ".jobs-search__job-details--container .jobs-box__html-content",
        ".jobs-search__job-details--container .jobs-description-content__text",
        ".jobs-description-content__text",
        "[data-job-id] .jobs-box__html-content",
        // Indeed
        "#jobDescriptionText",
        "[data-testid='jobsearch-JobComponent-description']",
        // ZipRecruiter
        ".job_description"
    ];

    for (const selector of selectors) {
        const element = document.querySelector(selector);

        if (element) {
            const text = normalizeText(element.innerText);

            if (text.length > 100) {
                return text;
            }
        }
    }

    // Last-resort fallback for job detail pages with semantic markup.
    const mainLikeElements = document.querySelectorAll("main, article, section");

    for (const element of mainLikeElements) {
        const text = normalizeText(element.innerText);

        if (looksLikeJobDescription(text)) {
            return text;
        }
    }

    return null;
}

function normalizeText(text) {
    if (!text) {
        return "";
    }

    return text
        .replace(/\s+/g, " ")
        .trim();
}

function looksLikeJobDescription(text) {
    if (!text || text.length < 300) {
        return false;
    }

    const lower = text.toLowerCase();

    return [
        "responsibilities",
        "qualifications",
        "requirements",
        "experience",
        "skills"
    ].some((keyword) => lower.includes(keyword));
}

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.type === "GET_JOB_DESCRIPTION") {
        sendResponse({
            description: extractJobDescription()
        });
    }
});

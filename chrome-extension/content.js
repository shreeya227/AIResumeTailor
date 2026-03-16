function extractJobDescription() {
    const selectors = [
        // LinkedIn variants
        ".jobs-description-content",
        ".jobs-box__html-content",
        ".jobs-description__content",
        ".jobs-search__job-details--container .jobs-box__html-content",
        ".jobs-search__job-details--container .jobs-description-content__text",
        ".jobs-description-content__text",
        ".jobs-search__job-details--container",
        ".job-view-layout",
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

function extractJobTitle() {
    const selectors = [
        ".job-details-jobs-unified-top-card__job-title",
        ".jobs-unified-top-card__job-title",
        ".jobs-details-top-card__job-title",
        ".job-details-jobs-unified-top-card__job-title-link",
        ".jobs-unified-top-card h1",
        ".top-card-layout__title",
        "[data-testid='jobsearch-JobInfoHeader-title']",
        "h1"
    ];

    return extractFirstMeaningfulText(selectors) || extractFromPageTitle().title;
}

function extractCompanyName() {
    const selectors = [
        ".job-details-jobs-unified-top-card__company-name",
        ".jobs-unified-top-card__company-name",
        ".jobs-details-top-card__company-url",
        ".job-details-jobs-unified-top-card__primary-description-container a",
        ".jobs-unified-top-card__primary-description a",
        ".topcard__flavor",
        "[data-testid='inlineHeader-companyName']",
        ".jobsearch-InlineCompanyRating div:first-child"
    ];

    return extractFirstMeaningfulText(selectors) || extractFromPageTitle().company;
}

function extractFirstMeaningfulText(selectors) {
    for (const selector of selectors) {
        const element = document.querySelector(selector);

        if (element) {
            const text = normalizeText(element.innerText);

            if (text.length > 1) {
                return text;
            }
        }
    }

    return null;
}

function extractJobData() {
    const description = extractJobDescription();

    if (!description) {
        return null;
    }

    return {
        description,
        title: extractJobTitle(),
        company: extractCompanyName()
    };
}

async function extractJobDataWithRetry(maxAttempts = 8, delayMs = 400) {
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
        const jobData = extractJobData();

        if (jobData) {
            return jobData;
        }

        await new Promise((resolve) => setTimeout(resolve, delayMs));
    }

    return {
        description: null,
        title: extractJobTitle(),
        company: extractCompanyName()
    };
}

function extractFromPageTitle() {
    const ogTitle = document.querySelector("meta[property='og:title']")?.content || "";
    const titleSource = ogTitle || document.title || "";

    const separators = [" at ", " | ", " - "];

    for (const separator of separators) {
        if (titleSource.includes(separator)) {
            const [title, company] = titleSource.split(separator, 2).map((part) => normalizeText(part));

            if (title && company) {
                return { title, company };
            }
        }
    }

    return { title: null, company: null };
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
        extractJobDataWithRetry().then(sendResponse);
        return true;
    }
});

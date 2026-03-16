const resultElement = document.getElementById("result");
const resumeStatusElement = document.getElementById("resumeStatus");

document.getElementById("analyzeJob")
    .addEventListener("click", async () => {
        resultElement.innerText = "Analyzing job description...";

        const jobDescription = await getJobDescription();

        if (!jobDescription) {
            resultElement.innerText = "Could not extract job description.";
            return;
        }

        try {
            const apiResponse = await fetch(
                "http://localhost:8080/jobs/analyze",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        description: jobDescription
                    })
                }
            );

            if (!apiResponse.ok) {
                throw new Error("Analyze request failed");
            }

            const analysis = await apiResponse.json();

            resultElement.innerText = [
                `Match Score: ${analysis.matchScore}%`,
                `Matched Skills: ${formatList(analysis.matchedSkills)}`,
                `Missing Skills: ${formatList(analysis.missingSkills)}`
            ].join("\n");
        } catch (error) {
            resultElement.innerText = "Job analysis failed.";
        }
    });

document.getElementById("generateResume")
    .addEventListener("click", async () => {
        resumeStatusElement.innerText = "Generating tailored resume...";

        const jobDescription = await getJobDescription();

        if (!jobDescription) {
            resumeStatusElement.innerText = "Could not extract job description.";
            return;
        }

        try {
            const apiResponse = await fetch(
                "http://localhost:8080/jobs/generate-resume",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        description: jobDescription
                    })
                }
            );

            if (!apiResponse.ok) {
                throw new Error("Resume generation request failed");
            }

            const blob = await apiResponse.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");

            a.href = url;
            a.download = getDownloadFileName(apiResponse.headers.get("Content-Disposition"));

            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);

            resumeStatusElement.innerText = "Resume downloaded successfully.";
        } catch (error) {
            resumeStatusElement.innerText = "Resume generation failed.";
        }
    });

async function getJobDescription() {
    let [tab] = await chrome.tabs.query({
        active: true,
        currentWindow: true
    });

    return new Promise((resolve) => {
        chrome.tabs.sendMessage(
            tab.id,
            { type: "GET_JOB_DESCRIPTION" },
            (response) => {
                if (!response || !response.description || response.description === "Job description not found") {
                    resolve(null);
                    return;
                }

                resolve(response.description);
            }
        );
    });
}

function formatList(items) {
    if (!items || items.length === 0) {
        return "None";
    }

    return items.join(", ");
}

function getDownloadFileName(contentDisposition) {
    if (!contentDisposition) {
        return "tailored_resume.pdf";
    }

    const match = contentDisposition.match(/filename="?([^"]+)"?/i);
    return match ? match[1] : "tailored_resume.pdf";
}

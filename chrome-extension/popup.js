const resultElement = document.getElementById("result");
const resumeStatusElement = document.getElementById("resumeStatus");
const progressContainer = document.getElementById("progressContainer");
const progressBar = document.getElementById("progressBar");

document.getElementById("analyzeJob")
    .addEventListener("click", async () => {
        resultElement.innerText = "Analyzing job description...";

        const jobData = await getJobData();

        if (!jobData) {
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
                        description: jobData.description,
                        title: jobData.title,
                        company: jobData.company
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
        setProgress(10, "Extracting job details...");

        const jobData = await getJobData();

        if (!jobData) {
            resetProgress("Could not extract job description.");
            return;
        }

        try {
            setProgress(30, "Sending resume request...");

            const apiResponse = await fetch(
                "http://localhost:8080/jobs/generate-resume",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        description: jobData.description,
                        title: jobData.title,
                        company: jobData.company
                    })
                }
            );

            if (!apiResponse.ok) {
                throw new Error("Resume generation request failed");
            }

            setProgress(75, "Building and downloading resume...");

            const blob = await apiResponse.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");

            a.href = url;
            a.download = getDownloadFileName(apiResponse.headers.get("Content-Disposition"));

            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);

            setProgress(100, "Resume downloaded successfully.");
            window.setTimeout(() => {
                progressContainer.classList.add("hidden");
            }, 1200);
        } catch (error) {
            resetProgress("Resume generation failed.");
        }
    });

async function getJobData() {
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

                resolve({
                    description: response.description,
                    title: response.title || "",
                    company: response.company || ""
                });
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

function setProgress(value, message) {
    progressContainer.classList.remove("hidden");
    progressContainer.setAttribute("aria-hidden", "false");
    progressBar.style.width = `${value}%`;
    resumeStatusElement.innerText = message;
}

function resetProgress(message) {
    progressBar.style.width = "0%";
    progressContainer.classList.add("hidden");
    progressContainer.setAttribute("aria-hidden", "true");
    resumeStatusElement.innerText = message;
}

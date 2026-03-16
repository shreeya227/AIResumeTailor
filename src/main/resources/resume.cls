\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{resume}[2026/03/15 Minimal resume class]

\LoadClass[11pt]{article}

\RequirePackage{hyperref}
\RequirePackage{titlesec}
\RequirePackage{enumitem}

\pagestyle{empty}
\setlength{\parindent}{0pt}
\setlength{\parskip}{0.25em}

\newcommand{\@resumeName}{}
\newcommand{\@resumeAddress}{}

\newcommand{\name}[1]{\gdef\@resumeName{#1}}
\newcommand{\address}[1]{\gdef\@resumeAddress{#1}}

\renewcommand{\maketitle}{%
  \begin{center}
    {\LARGE\bfseries \@resumeName\par}
    \vspace{0.35em}
    {\small \@resumeAddress\par}
  \end{center}
  \vspace{0.5em}
}

\AtBeginDocument{\maketitle}

\titleformat{\section}{\large\bfseries\uppercase}{}{0em}{}[\titlerule]
\titlespacing*{\section}{0pt}{0.8em}{0.4em}

\newenvironment{rSection}[1]{\section*{#1}}{}

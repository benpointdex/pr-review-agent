package com.henry.git_pr.agent;

import com.henry.git_pr.model.domain.*;
import java.util.List;
import java.util.stream.Collectors;



public class PromptBuilder {

    // ─── SECURITY SCAN ───────────────────────────────────────
    public static String securityPrompt(FileContext context, RepoKnowledge knowledge) {
        String filesText = buildFilesText(context.productionFiles());

        String knowledgeSection = knowledge.isEmpty()
                ? "(No previous knowledge about this repo yet)"
                : knowledge.conventions();

        return """
            You are a security-focused code reviewer. Your job is to find ONE most critical
            security issue in the changed code, or report that none exists.
            
            PREVIOUS KNOWLEDGE ABOUT THIS REPO:
            %s
            
            CHANGED FILES (production code only):
            %s
            
            WHAT TO LOOK FOR:
            - SQL injection, XSS, command injection vulnerabilities
            - Exposed API keys, passwords, secrets hardcoded in code
            - Missing authentication or authorization checks
            - Insecure deserialization
            - Path traversal vulnerabilities
            - Missing input validation on user-controlled data
            - Insecure direct object references
            
            RULES:
            - Focus ONLY on security issues. Ignore style, logic bugs, and test coverage.
            - Be specific. Include the exact file path and line number.
            - If you find nothing, return severity=INFO and say "No security issues found."
            - Do NOT make up issues that aren't there.
            
            Return a JSON object with fields: severity (INFO/WARNING/CRITICAL), filePath, lineNumber (-1 if not applicable), description, suggestion.
            """.formatted(knowledgeSection, filesText);
    }

    // ─── LOGIC REVIEW ────────────────────────────────────────
    public static String logicPrompt(FileContext context, RepoKnowledge knowledge) {
        String filesText = buildFilesText(context.files());

        String knowledgeSection = knowledge.isEmpty()
                ? "(No previous knowledge about this repo yet)"
                : knowledge.conventions();

        return """
            You are a senior software engineer reviewing code for correctness.
            Find the ONE most important logic issue in this PR, or confirm it looks correct.
            
            PR CONTEXT AND REPO CONVENTIONS:
            %s
            
            CHANGED FILES:
            %s
            
            WHAT TO LOOK FOR:
            - NullPointerException risks (accessing fields on potentially null objects)
            - Incorrect conditional logic (wrong operator, wrong comparison)
            - Off-by-one errors in loops or array access
            - Race conditions or thread safety issues
            - Wrong assumptions about state (assuming something is set when it might not be)
            - Edge cases not handled (empty lists, zero values, negative numbers)
            - Logic that doesn't match what the PR title says it should do
            
            RULES:
            - Think about what the code is TRYING to do vs what it ACTUALLY does.
            - Be specific. Include file path and line number.
            - Ignore security issues and test coverage — other reviewers handle those.
            - If the logic looks correct, return severity=INFO and say "Logic looks correct."
            Return a JSON object with fields: severity (INFO/WARNING/CRITICAL), filePath, lineNumber (-1 if not applicable), description, suggestion.
            """.formatted(knowledgeSection, filesText);
    }

    // ─── COVERAGE CHECK ──────────────────────────────────────
    public static String coveragePrompt(FileContext context, RepoKnowledge knowledge) {
        String productionText = buildFilesText(context.productionFiles());
        String testText = context.testFiles().isEmpty()
                ? "(No test files changed in this PR)"
                : buildFilesText(context.testFiles());

        return """
            You are reviewing test coverage for a pull request.
            
            PRODUCTION CODE CHANGED:
            %s
            
            TEST CODE CHANGED:
            %s
            
            WHAT TO CHECK:
            - Are there new functions or methods with no corresponding tests?
            - Are there new code paths (if/else branches) that tests don't cover?
            - If the PR adds error handling, is there a test for the error case?
            - If the PR adds an API endpoint, is there an integration test?
            
            RULES:
            - If no test files were changed but production code was added, that's a WARNING.
            - If test files were changed and cover the main paths, that's INFO.
            - Be specific about which functions or paths are missing tests.
            - Don't require 100%% coverage — focus on critical untested paths.
            Return a JSON object with fields: severity (INFO/WARNING/CRITICAL), filePath, lineNumber (-1 if not applicable), description, suggestion.
            """.formatted(productionText, testText);
    }

    // ─── SYNTHESIS ───────────────────────────────────────────
    public static String synthesisPrompt(
            SecurityFinding security,
            LogicFinding logic,
            CoverageFinding coverage,
            RepoKnowledge knowledge,
            PullRequestEvent event
    ) {
        String findingsText = """
            - **SECURITY (%s)** — %s: %s (Suggestion: %s)
            - **LOGIC (%s)** — %s: %s (Suggestion: %s)
            - **COVERAGE (%s)** — %s: %s (Suggestion: %s)
            """.formatted(
                security.severity(), security.filePath(), security.description(), security.suggestion(),
                logic.severity(), logic.filePath(), logic.description(), logic.suggestion(),
                coverage.severity(), coverage.filePath(), coverage.description(), coverage.suggestion()
        );

        return """
            You are a senior engineer writing a final code review for a GitHub PR.
            
            PR TITLE: %s
            REPO: %s
            AUTHOR: %s
            
            SPECIALIST FINDINGS:
            %s
            
            RULES FOR VERDICT:
            - BLOCK: any finding with severity=CRITICAL
            - FLAG:  any finding with severity=WARNING (and no CRITICAL)
            - PASS:  all findings are INFO
            
            OUTPUT FORMAT (GitHub markdown, will be posted as a PR comment):
            
            Start with: "🤖 **PR Review Agent**"
            
            Then write a 2-3 sentence summary of what the PR does and overall quality.
            
            Then write: "**Verdict: [emoji] [VERDICT]**"
            Use 🔴 for BLOCK, 🟡 for FLAG, 🟢 for PASS.
            
            Then add "---" as a divider.
            
            Then for each finding (skip INFO findings unless all are INFO):
            - Use ### with an emoji: 🔴 for CRITICAL, 🟡 for WARNING, 🟢 for INFO
            - Include: finding type, file path, line number (if available)
            - Write the description in plain English
            - Write a clear, actionable suggestion
            
            End with: "*Reviewed by PR Review Agent | Embabel + Gemini*"
            
            TONE: Direct, technical, respectful. No filler phrases. Be specific.
            Return a FinalReview JSON with fields:
            verdict (PASS/FLAG/BLOCK), summary (plain text, 2-3 sentences),
            formattedComment (the complete GitHub markdown string ready to POST).
            """.formatted(
                event.prTitle(),
                event.repoFullName(),
                event.authorLogin(),
                findingsText
        );
    }

    // ─── HELPER ──────────────────────────────────────────────
    private static String buildFilesText(List<ChangedFile> files) {
        if (files == null || files.isEmpty()) {
            return "(no files)";
        }
        return files.stream()
                .filter(f -> !f.patch().isBlank())
                .map(f -> """
                === FILE: %s (status: %s, +%d -%d) ===
                
                DIFF (changes only):
                %s
                
                FULL CONTENT:
                %s
                """.formatted(
                        f.filename(), f.status(),
                        f.additions(), f.deletions(),
                        f.patch(),
                        f.fullContent().isBlank() ? "(not available)" : f.fullContent()
                ))
                .collect(Collectors.joining("\n\n"));
    }
}
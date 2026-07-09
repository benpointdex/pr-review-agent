package com.henry.git_pr.service;

import com.henry.git_pr.model.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final GitHub gitHub;

    /**
     * Fetch the list of changed files in a PR (including additions, deletions, and git patches).
     */
    public Diff fetchDiff(String repoFullName, int prNumber) {
        try {
            log.info("Fetching diff for PR #{} in {}", prNumber, repoFullName);

            GHRepository repo = gitHub.getRepository(repoFullName);
            GHPullRequest pr = repo.getPullRequest(prNumber);

            List<GHPullRequestFileDetail> ghFiles = pr.listFiles().toList();
            List<ChangedFile> changedFiles = ghFiles.stream()
                    .map(ghFile -> new ChangedFile(
                            ghFile.getFilename(),
                            ghFile.getStatus(),
                            ghFile.getPatch() != null ? ghFile.getPatch() : "",
                            "", // fullContent is loaded asynchronously in fetchFileContext
                            ghFile.getAdditions(),
                            ghFile.getDeletions()
                    ))
                    .collect(Collectors.toList());

            int totalAdditions = changedFiles.stream().mapToInt(ChangedFile::additions).sum();
            int totalDeletions = changedFiles.stream().mapToInt(ChangedFile::deletions).sum();

            log.info("Fetched diff for PR #{}: {} files changed (+{} -{})",
                    prNumber, changedFiles.size(), totalAdditions, totalDeletions);

            return new Diff(repoFullName, prNumber, changedFiles, totalAdditions, totalDeletions, changedFiles.size());

        } catch (IOException e) {
            log.error("Failed to fetch diff for PR #{} in {}", prNumber, repoFullName, e);
            throw new RuntimeException("GitHub API error fetching diff: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the full content for all changed files in parallel.
     * Separates them into production and test files.
     */
    public FileContext fetchFileContext(Diff diff, String headSha) {
        log.info("Fetching full file content for {} files in parallel", diff.files().size());

        try {
            GHRepository repo = gitHub.getRepository(diff.repoFullName());

            // 1. Fetch file contents in parallel using CompletableFuture
            List<CompletableFuture<ChangedFile>> futures = diff.files().stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> fetchSingleFileContent(repo, file, headSha)))
                    .toList();

            // Join all parallel tasks
            List<ChangedFile> enrichedFiles = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 2. Separate into production and test files
            List<ChangedFile> testFiles = enrichedFiles.stream()
                    .filter(f -> isTestFile(f.filename()))
                    .collect(Collectors.toList());

            List<ChangedFile> productionFiles = enrichedFiles.stream()
                    .filter(f -> !isTestFile(f.filename()))
                    .collect(Collectors.toList());

            log.info("File context ready: {} production files, {} test files",
                    productionFiles.size(), testFiles.size());

            return new FileContext(
                    diff.repoFullName(),
                    diff.prNumber(),
                    enrichedFiles,
                    productionFiles,
                    testFiles
            );

        } catch (IOException e) {
            log.error("Failed to fetch repository metadata for {}", diff.repoFullName(), e);
            throw new RuntimeException("GitHub API error fetching file context: " + e.getMessage(), e);
        }
    }

    /**
     * Posts a comment to the pull request.
     * Returns the generated GitHub comment ID.
     */
    public long postReviewComment(String repoFullName, int prNumber, String commentBody) {
        try {
            log.info("Posting review comment to PR #{} in {}", prNumber, repoFullName);

            GHRepository repo = gitHub.getRepository(repoFullName);
            GHPullRequest pr = repo.getPullRequest(prNumber);
            GHIssueComment comment = pr.comment(commentBody);

            log.info("Successfully posted comment ID: {} to PR #{}", comment.getId(), prNumber);
            return comment.getId();

        } catch (IOException e) {
            log.error("Failed to post review comment to PR #{}", prNumber, e);
            throw new RuntimeException("GitHub API error posting comment: " + e.getMessage(), e);
        }
    }

    // Helper: Fetch a single file content from GitHub and handle decoding / truncation
    private ChangedFile fetchSingleFileContent(GHRepository repo, ChangedFile file, String headSha) {
        // Skip deleted files or files with no changes (e.g. binary or renamed only)
        if ("deleted".equals(file.status()) || file.patch().isBlank()) {
            return file;
        }

        String fullContent = "";
        try {
            GHContent content = repo.getFileContent(file.filename(), headSha);

            // GitHub returns content base64 encoded
            byte[] decoded = Base64.getMimeDecoder().decode(content.getContent().replace("\n", ""));
            fullContent = new String(decoded, StandardCharsets.UTF_8);

            // Truncate files that are too large (e.g., over 8000 chars) to prevent hitting LLM context limits
            if (fullContent.length() > 8000) {
                fullContent = fullContent.substring(0, 8000) + "\n... (truncated for context length)";
            }

        } catch (IOException e) {
            log.warn("Could not fetch full content for: {} (ref: {}). Fallback to empty context.", file.filename(), headSha);
            fullContent = "(content unavailable)";
        }

        return new ChangedFile(
                file.filename(),
                file.status(),
                file.patch(),
                fullContent,
                file.additions(),
                file.deletions()
        );
    }

    private boolean isTestFile(String filename) {
        String lower = filename.toLowerCase();

        // 1. Matches standard test directories in Maven, Gradle, Node, Python, Go, etc.
        if (lower.contains("/test/") || lower.contains("/tests/")) {
            return true;
        }

        // 2. Matches naming conventions across multiple languages
        return lower.contains("test.java") || lower.contains("spec.java") // Java
                || lower.endsWith("test.kt")   || lower.endsWith("spec.kt")   // Kotlin
                || lower.endsWith(".test.js")  || lower.endsWith(".spec.js")  // JavaScript
                || lower.endsWith(".test.ts")  || lower.endsWith(".spec.ts")  // TypeScript
                || lower.contains("test_")     || lower.endsWith("_test.py")  // Python
                || lower.endsWith("_test.go")                                 // Go
                || lower.endsWith("_spec.rb")  || lower.startsWith("test_")   // Ruby
                || lower.endsWith("tests.cs")  || lower.endsWith("test.cs");  // C#
    }

}

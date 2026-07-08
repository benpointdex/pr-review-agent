package com.henry.git_pr.model.domain;

public record PullRequestEvent(
    String repoFullName,    // "owner/repo-name" e.g. "henry/my-project"
    int    prNumber,        // PR number e.g. 42
    String prTitle,         // "Add payment refund endpoint"
    String baseBranch,      // "main" — the branch being merged INTO
    String headBranch,      // "feature/payment-refund" — the PR branch
    String headSha,         // commit SHA of the PR's latest commit
    String authorLogin,     // GitHub username of who opened the PR
    String action           // "opened" or "synchronize"
) {}

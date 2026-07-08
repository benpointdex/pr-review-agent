package com.henry.git_pr.model.domain;

public record ReviewedPR(
    String repoFullName,
    int    prNumber,
    Verdict verdict,
    long   githubCommentId,     // the comment ID GitHub returned
    long   processingTimeMs     // how long the whole review took
) {}

package com.henry.git_pr.model.domain;

public record ChangedFile(
    String filename,        // "src/main/java/PaymentController.java"
    String status,          // "added", "modified", "deleted", "renamed"
    String patch,           // the raw git diff patch for this file
    String fullContent,     // the FULL file content (not just changed lines)
    int    additions,       // number of lines added
    int    deletions        // number of lines deleted
) {}

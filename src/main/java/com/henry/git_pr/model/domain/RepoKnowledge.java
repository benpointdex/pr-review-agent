package com.henry.git_pr.model.domain;

public record RepoKnowledge(
    String repoFullName,
    String conventions,     // markdown text: conventions, patterns, past issues
    int    reviewCount      // how many times we've reviewed this repo
) {
    // Factory method for when we've never seen this repo before
    public static RepoKnowledge empty(String repoFullName) {
        return new RepoKnowledge(repoFullName, "", 0);
    }

    // Returns true if we have no knowledge yet
    public boolean isEmpty() {
        return conventions == null || conventions.isBlank();
    }
}

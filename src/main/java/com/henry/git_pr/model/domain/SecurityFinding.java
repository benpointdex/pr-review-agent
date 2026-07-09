package com.henry.git_pr.model.domain;

public record SecurityFinding(
    Severity severity,
    String filePath,
    int lineNumber,
    String description,
    String suggestion
) {}

package com.henry.git_pr.model.domain;

public record LogicFinding(
    Severity severity,
    String filePath,
    int lineNumber,
    String description,
    String suggestion
) {}

package com.henry.git_pr.model.domain;

public record CoverageFinding(
    Severity severity,
    String filePath,
    int lineNumber,
    String description,
    String suggestion
) {}

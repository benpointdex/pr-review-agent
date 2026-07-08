package com.henry.git_pr.model.domain;

import java.util.List;

public record FileContext(
    String            repoFullName,
    int               prNumber,
    List<ChangedFile> files,           // same files but now with fullContent populated
    List<ChangedFile> productionFiles, // only non-test files
    List<ChangedFile> testFiles        // only test files (useful for coverage check)
) {}

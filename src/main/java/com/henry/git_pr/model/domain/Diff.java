package com.henry.git_pr.model.domain;

import java.util.List;

public record Diff(
    String          repoFullName,
    int             prNumber,
    List<ChangedFile> files,
    int             totalAdditions,
    int             totalDeletions,
    int             totalChangedFiles
) {}

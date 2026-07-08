package com.henry.git_pr.model.domain;

public enum Severity {
    INFO,       // nice to know, no action required
    WARNING,    // should fix before merging
    CRITICAL    // must fix, will BLOCK the PR
}

package com.henry.git_pr.model.domain;

public enum Verdict {
    PASS,   // looks good, safe to merge
    FLAG,   // has warnings, review them but can merge
    BLOCK   // has critical issues, do not merge
}

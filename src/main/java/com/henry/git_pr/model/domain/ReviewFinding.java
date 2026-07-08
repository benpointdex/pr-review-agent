package com.henry.git_pr.model.domain;

public record ReviewFinding(
    FindingType type,           // SECURITY, LOGIC, or COVERAGE
    Severity    severity,       // INFO, WARNING, or CRITICAL
    String      filePath,       // "src/main/java/PaymentController.java"
    int         lineNumber,     // 47 — the specific line, or -1 if no specific line
    String      description,    // what the issue is, in plain English
    String      suggestion      // how to fix it
) {}

package com.henry.git_pr.model.domain;

public record FinalReview(
    Verdict verdict,            // PASS, FLAG, or BLOCK
    String  summary,            // 2-3 sentence overview
    String  formattedComment   // ready-to-post GitHub markdown
) {}

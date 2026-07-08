package com.henry.git_pr.model.domain;

import java.util.List;

public record FinalReview(
    Verdict          verdict,            // PASS, FLAG, or BLOCK
    String           summary,            // 2-3 sentence overview
    List<ReviewFinding> findings,        // all individual findings
    String           formattedComment   // ready-to-post GitHub markdown
) {}

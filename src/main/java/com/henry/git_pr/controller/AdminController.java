package com.henry.git_pr.controller;

import com.henry.git_pr.repository.PrReviewRepository;
import com.henry.git_pr.repository.RepoKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PrReviewRepository       prReviewRepository;
    private final RepoKnowledgeRepository  repoKnowledgeRepository;

    /**
     * GET /admin/health
     *
     * Quick check that the app is running and database is connected.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "totalReviews", prReviewRepository.count(),
                "totalReposLearned", repoKnowledgeRepository.count()
        ));
    }
}
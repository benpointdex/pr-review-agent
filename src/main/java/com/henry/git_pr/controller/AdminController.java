package com.henry.git_pr.controller;

import com.henry.git_pr.model.domain.PullRequestEvent;
import com.henry.git_pr.model.entity.PrReviewEntity;
import com.henry.git_pr.model.entity.RepoKnowledgeEntity;
import com.henry.git_pr.repository.PrReviewRepository;
import com.henry.git_pr.repository.RepoKnowledgeRepository;
import com.henry.git_pr.service.AgentTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AgentTriggerService     agentTriggerService;
    private final PrReviewRepository      prReviewRepository;
    private final RepoKnowledgeRepository repoKnowledgeRepository;

    // ─────────────────────────────────────────────
    // POST /admin/trigger
    // Use this to test the agent without needing
    // a real GitHub webhook to fire.
    // ─────────────────────────────────────────────
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerManually(@RequestBody Map<String, Object> body) {

        PullRequestEvent event = new PullRequestEvent(
                (String)  body.get("repoFullName"),
                (Integer) body.get("prNumber"),
                (String)  body.getOrDefault("prTitle",    "Manual trigger"),
                (String)  body.getOrDefault("baseBranch", "main"),
                (String)  body.getOrDefault("headBranch", "feature/test"),
                (String)  body.get("headSha"),
                (String)  body.getOrDefault("author",     "test-user"),
                "opened"
        );

        log.info("Manual trigger for PR #{} in {}", event.prNumber(), event.repoFullName());
        String processId = agentTriggerService.triggerReview(event);

        return ResponseEntity.ok(
                "Review triggered for PR #" + event.prNumber() + " — processId: " + processId
        );
    }

    // ─────────────────────────────────────────────
    // GET /admin/reviews
    // GET /admin/reviews?repo=owner/repo-name
    // ─────────────────────────────────────────────
    @GetMapping("/reviews")
    public ResponseEntity<List<PrReviewEntity>> getReviews(
            @RequestParam(required = false) String repo
    ) {
        List<PrReviewEntity> reviews = repo != null
                ? prReviewRepository.findByRepoFullNameOrderByCreatedAtDesc(repo)
                : prReviewRepository.findTop20ByOrderByCreatedAtDesc();

        return ResponseEntity.ok(reviews);
    }

    // ─────────────────────────────────────────────
    // GET /admin/knowledge?repo=owner/repo-name
    // ─────────────────────────────────────────────
    @GetMapping("/knowledge")
    public ResponseEntity<RepoKnowledgeEntity> getKnowledge(@RequestParam String repo) {
        return repoKnowledgeRepository.findByRepoFullName(repo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────
    // DELETE /admin/knowledge?repo=owner/repo-name
    // Resets agent memory for a repo — useful when
    // you want to test fresh knowledge accumulation
    // ─────────────────────────────────────────────
    @DeleteMapping("/knowledge")
    public ResponseEntity<String> resetKnowledge(@RequestParam String repo) {
        repoKnowledgeRepository.findByRepoFullName(repo)
                .ifPresent(repoKnowledgeRepository::delete);
        return ResponseEntity.ok("Knowledge reset for: " + repo);
    }

    // ─────────────────────────────────────────────
    // GET /admin/health
    // Quick check that the app is running and DB
    // is connected
    // ─────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status",           "UP",
                "totalReviews",     prReviewRepository.count(),
                "totalReposLearned", repoKnowledgeRepository.count()
        ));
    }
}
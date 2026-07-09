package com.henry.git_pr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henry.git_pr.model.domain.PullRequestEvent;
import com.henry.git_pr.service.WebhookSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSignatureService signatureService;
    private final ObjectMapper            objectMapper;
    // Phase 2: private final AgentTriggerService agentTriggerService;

    /**
     * POST /webhook/github
     *
     * Called by: GitHub (automatically when a PR is opened/updated)
     * Headers:   X-GitHub-Event: pull_request
     *            X-Hub-Signature-256: sha256=<hmac>
     * Body:      GitHub pull_request webhook payload (JSON)
     */
    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event")       String eventType,
            @RequestHeader("X-Hub-Signature-256")  String signature,
            @RequestBody                           String rawPayload
    ) {
        log.info("Received GitHub webhook event: {}", eventType);

        // Step 1: Verify this request actually came from GitHub
        if (!signatureService.isValidSignature(rawPayload, signature)) {
            log.warn("Invalid webhook signature rejected");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // Step 2: Only process pull_request events
        if (!"pull_request".equals(eventType)) {
            log.debug("Ignoring non-PR event: {}", eventType);
            return ResponseEntity.ok("Event ignored");
        }

        // Step 3: Parse the payload
        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String action    = payload.get("action").asText();

            // Only trigger on "opened" (new PR) or "synchronize" (new commits pushed)
            List<String> triggerActions = List.of("opened", "synchronize");
            if (!triggerActions.contains(action)) {
                log.debug("Ignoring PR action: {}", action);
                return ResponseEntity.ok("PR action ignored: " + action);
            }

            // Step 4: Build our domain object from the GitHub payload
            PullRequestEvent event = parsePullRequestEvent(payload, action);

            log.info("PR event parsed: PR #{} in {} by {} (action: {})",
                    event.prNumber(), event.repoFullName(), event.authorLogin(), action);

            // Phase 2: agentTriggerService.triggerReviewAsync(event);

            return ResponseEntity.ok("Review triggered for PR #" + event.prNumber());

        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            return ResponseEntity.status(500).body("Error processing webhook");
        }
    }

    private PullRequestEvent parsePullRequestEvent(JsonNode payload, String action) {
        JsonNode pr   = payload.get("pull_request");
        JsonNode repo = payload.get("repository");
        JsonNode head = pr.get("head");
        JsonNode base = pr.get("base");

        return new PullRequestEvent(
                repo.get("full_name").asText(),
                pr.get("number").asInt(),
                pr.get("title").asText(),
                base.get("ref").asText(),
                head.get("ref").asText(),
                head.get("sha").asText(),
                pr.get("user").get("login").asText(),
                action
        );
    }
}
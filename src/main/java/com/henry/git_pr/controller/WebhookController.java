package com.henry.git_pr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henry.git_pr.model.domain.PullRequestEvent;
import com.henry.git_pr.service.AgentTriggerService;
import com.henry.git_pr.service.WebhookSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSignatureService signatureService;
    private final AgentTriggerService     agentTriggerService; // ← uncommented
    private final ObjectMapper            objectMapper;

    @PostMapping("/github")
    public ResponseEntity<?> handleGitHubWebhook(
            @RequestHeader("X-GitHub-Event")       String eventType,
            @RequestHeader("X-Hub-Signature-256")  String signature,
            @RequestBody                           String rawPayload
    ) {
        log.info("Received GitHub webhook event: {}", eventType);

        if (!signatureService.isValidSignature(rawPayload, signature)) {
            log.warn("Invalid webhook signature rejected");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        if (!"pull_request".equals(eventType)) {
            log.debug("Ignoring non-PR event: {}", eventType);
            return ResponseEntity.ok("Event ignored");
        }

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String action    = payload.get("action").asText();

            if (!List.of("opened", "synchronize").contains(action)) {
                log.debug("Ignoring PR action: {}", action);
                return ResponseEntity.ok("PR action ignored: " + action);
            }

            PullRequestEvent event = parsePullRequestEvent(payload, action);

            log.info("PR event parsed: PR #{} in {} by {} (action: {})",
                    event.prNumber(), event.repoFullName(), event.authorLogin(), action);

            // ← actual trigger now wired in
            String processId = agentTriggerService.triggerReview(event);

            // Return 202 Accepted + process ID immediately
            // GitHub doesn't wait for the review to finish
            return ResponseEntity.accepted().body(Map.of(
                    "message",   "Review triggered for PR #" + event.prNumber(),
                    "processId", processId,
                    "statusUrl", "/api/v1/process/" + processId,
                    "sseUrl",    "/events/process/" + processId
            ));

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
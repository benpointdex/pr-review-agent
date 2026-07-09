package com.henry.git_pr.service;



import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.henry.git_pr.agent.PRReviewAgent;
import com.henry.git_pr.model.domain.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTriggerService {

    private final AgentPlatform agentPlatform;

    public String triggerReview(PullRequestEvent event) {
        log.info("Triggering agent for PR #{} in {}",
                event.prNumber(), event.repoFullName());
        Agent agent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("PRReviewAgent"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "PRReviewAgent not found — is @Agent registered and @EnableAgents on main class?"));

        // Step 2: Create the process — binds PullRequestEvent to blackboard by type
        AgentProcess agentProcess =agentPlatform.createAgentProcessFrom(
                agent ,
                ProcessOptions.DEFAULT,
                event
        );

        // Step 3: Start non-blocking — returns immediately
        // Agent runs in background, Embabel manages the thread
        agentPlatform.start(agentProcess);
        log.info("Agent process {} started for PR #{}",
                agentProcess.getId(), event.prNumber());

        // Return process ID — caller can poll built-in Embabel endpoints:
        // GET  /api/v1/process/{processId}   → status + result
        // GET  /events/process/{processId}   → SSE live stream
        return agentProcess.getId();

    }
} // (Only one closing bracket here!)

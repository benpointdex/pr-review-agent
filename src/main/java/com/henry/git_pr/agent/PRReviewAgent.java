package com.henry.git_pr.agent;


import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.henry.git_pr.model.domain.*;
import com.henry.git_pr.service.GitHubService;
import com.henry.git_pr.service.RepoKnowledgeService;
import com.henry.git_pr.service.ReviewPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Agent(name = "PRReviewAgent", description = """
    Reviews GitHub pull requests autonomously.
    Given a PullRequestEvent, fetches the diff, runs specialist sub-agents
    for security, logic, and coverage analysis in parallel, synthesizes the
    findings, and posts a structured review comment on the PR.
    """)

@RequiredArgsConstructor
public class PRReviewAgent {


    private final GitHubService            gitHubService;
    private final RepoKnowledgeService repoKnowledgeService;
    private final ReviewPersistenceService reviewPersistenceService;


    // ACTION 1: FETCH DIFF
    @Action(description = "Fetch the pull request diff from GitHub API")
    public Diff fetchDiff(PullRequestEvent event){

        log.info("ACTION: fetchDiff for PR #{} in {}", event.prNumber(), event.repoFullName());
        return gitHubService.fetchDiff(event.repoFullName(), event.prNumber());
    }

    // ACTION 2: FETCH FILE CONTEXT
    @Action(description = "Fetch full file contents for all changed files")
    public FileContext fetchContext (PullRequestEvent event , Diff diff){

        log.info("ACTION: fetchFileContext for {} files", diff.totalChangedFiles());
        return gitHubService.fetchFileContext(diff , event.headSha());
    }

    // ACTION 3: LOAD REPO KNOWLEDGE (can run in parallel with fetchDiff)
    @Action(description = "Load what the agent has previously learned about this repository")

    public RepoKnowledge loadRepoKnowledge (PullRequestEvent event){
        log.info("ACTION: loadRepoKnowledge for {}", event.repoFullName());
        return repoKnowledgeService.loadKnowledge(event.repoFullName());

    }

    @Action(description = "Scan changed code for security vulnerabilities using AI")
    public SecurityFinding runSecurityScan(FileContext fileContext , RepoKnowledge repoKnowledge , Ai ai) {
        log.info("ACTION: runSecurityScan");
        String prompt = PromptBuilder.securityPrompt(fileContext, repoKnowledge);

        return ai
                .withLlmByRole("fast")
                .createObject(prompt , SecurityFinding.class);

    }

    // ACTION 4b: LOGIC REVIEW (parallel with 4a and 4c)
    @Action(description = "Review changed code for logic bugs and correctness using AI")
    public LogicFinding runLogicReview(FileContext fileContext , RepoKnowledge repoKnowledge , Ai ai){

        log.info("ACTION: runLogicReview");
        String prompt = PromptBuilder.logicPrompt(fileContext, repoKnowledge);
        return ai
                .withLlmByRole("fast")
                .createObject(prompt , LogicFinding.class);

    }



    // ACTION 4c: COVERAGE CHECK (parallel with 4a and 4b)
    @Action(description = "Check if the changed code has adequate test coverage")
    public CoverageFinding runCoverageCheck (FileContext fileContext , RepoKnowledge repoKnowledge , Ai ai){

        log.info("ACTION: runCoverageCheck");
        String prompt = PromptBuilder.coveragePrompt(fileContext, repoKnowledge);
        return ai
                .withLlmByRole("fast")
                .createObject(prompt , CoverageFinding.class);


    }

    // ACTION 5: SYNTHESIZE REVIEW
    @Action(description = "Synthesize all findings into a final structured PR review")

    public FinalReview synthesizeReview (SecurityFinding securityFinding,
                                         LogicFinding logicFinding,
                                         CoverageFinding coverageFinding,
                                         RepoKnowledge repoKnowledge,
                                         PullRequestEvent event,
                                         Ai ai){

        log.info("ACTION: synthesizeReview");
        String prompt = PromptBuilder.synthesisPrompt( securityFinding, logicFinding, coverageFinding, repoKnowledge, event);
        return ai
                .withLlmByRole("best")
                .createObject(prompt , FinalReview.class);

    }

    // ACTION 6: POST AND LEARN (THE GOAL)
    @AchievesGoal(description = "PR review has been posted to GitHub and knowledge updated")
    @Action(description = "Post the review comment to GitHub and update repo knowledge")
    public  ReviewedPR reviewedPR(
            PullRequestEvent event,
            RepoKnowledge repoKnowledge,
            FinalReview finalReview,
            SecurityFinding securityFinding,
            LogicFinding logicFinding,
            CoverageFinding coverageFinding
    ) {


        log.info("ACTION: postAndLearn — posting verdict: {}", finalReview.verdict());
        long startTime = Instant.now().toEpochMilli();

        // 1. Post the comment to GitHub

        long commentId = gitHubService.postReviewComment(event.repoFullName(), event.prNumber(), finalReview.formattedComment());

        long processingTime = Instant.now().toEpochMilli() - startTime;

        // 2. Update the per-repo knowledge for next time
        repoKnowledgeService.updateKnowledge(event.repoFullName(), finalReview, securityFinding, logicFinding, coverageFinding, repoKnowledge);

        // 3. Save the review to our database (audit trail)
        ReviewedPR result = new ReviewedPR(
                event.repoFullName(), event.prNumber(), finalReview.verdict(), commentId, processingTime);

        reviewPersistenceService.saveReview(event, finalReview, securityFinding, logicFinding, coverageFinding, result);

        log.info("DONE: Review posted for PR #{} — Verdict: {}, Comment ID: {}",
                event.prNumber(), finalReview.verdict(), commentId);

        return result;
    }
}

package com.henry.git_pr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.henry.git_pr.model.domain.*;
import com.henry.git_pr.model.entity.PrReviewEntity;
import com.henry.git_pr.repository.PrReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPersistenceService {

    private final PrReviewRepository prReviewRepository;
    private final ObjectMapper       objectMapper;

    @Transactional
    public void saveReview(
            PullRequestEvent event,
            FinalReview review,
            SecurityFinding security,
            LogicFinding logic,
            CoverageFinding coverage,
            ReviewedPR result
    ) {
        try {
            PrReviewEntity entity = new PrReviewEntity();
            entity.setRepoFullName(event.repoFullName());
            entity.setPrNumber(event.prNumber());
            entity.setPrTitle(event.prTitle());
            entity.setPrAuthor(event.authorLogin());
            entity.setVerdict(review.verdict().name());
            entity.setSummary(review.summary());
            entity.setFormattedComment(review.formattedComment());
            entity.setGithubCommentId(result.githubCommentId());
            entity.setProcessingTimeMs(result.processingTimeMs());

            // Store each finding as JSON
            try {
                if (security != null) {
                    entity.setSecurityFinding(objectMapper.writeValueAsString(security));
                }
                if (logic != null) {
                    entity.setLogicFinding(objectMapper.writeValueAsString(logic));
                }
                if (coverage != null) {
                    entity.setCoverageFinding(objectMapper.writeValueAsString(coverage));
                }
            } catch (Exception e) {
                log.warn("Could not serialize findings: {}", e.getMessage());
            }

            prReviewRepository.save(entity);
            log.info("Saved review to DB for PR #{}", event.prNumber());

        } catch (Exception e) {
            log.error("Failed to save review to DB for PR #{}", event.prNumber(), e);
        }
    }
}
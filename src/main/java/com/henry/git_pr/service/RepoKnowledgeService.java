package com.henry.git_pr.service;

import com.henry.git_pr.model.domain.*;
import com.henry.git_pr.model.entity.RepoKnowledgeEntity;
import com.henry.git_pr.repository.RepoKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepoKnowledgeService {

    private final RepoKnowledgeRepository repository;

    public RepoKnowledge loadKnowledge(String repoFullName) {
        return repository.findByRepoFullName(repoFullName)
                .map(entity -> new RepoKnowledge(
                        entity.getRepoFullName(),
                        entity.getConventions(),
                        entity.getReviewCount()
                ))
                .orElseGet(() -> {
                    log.info("First time seeing repo: {} — starting with empty knowledge", repoFullName);
                    return RepoKnowledge.empty(repoFullName);
                });
    }

    @Transactional
    public void updateKnowledge(
            String repoFullName,
            FinalReview review,
            SecurityFinding security,
            LogicFinding logic,
            CoverageFinding coverage,
            RepoKnowledge existing
    ) {

        boolean hasNoteworthy = (security != null && security.severity() != Severity.INFO)
                || (logic != null && logic.severity() != Severity.INFO)
                || (coverage != null && coverage.severity() != Severity.INFO);

        if (!hasNoteworthy && existing.reviewCount() > 0) {
            repository.findByRepoFullName(repoFullName)
                    .ifPresent(entity -> {
                        entity.setReviewCount(entity.getReviewCount() + 1);
                        entity.setLastUpdated(LocalDateTime.now());
                        repository.save(entity);
                    });
            return;
        }

        String existingConventions = existing.conventions();
        String newEntry = buildKnowledgeEntry(review, security, logic, coverage);
        String updatedConventions = existingConventions.isBlank()
                ? newEntry
                : existingConventions + "\n\n" + newEntry;

        // Keep max 5000 chars — trim oldest entries if needed
        if (updatedConventions.length() > 5000) {
            updatedConventions = "...(earlier history trimmed)...\n\n"
                    + updatedConventions.substring(updatedConventions.length() - 5000);
        }

        String finalConventions = updatedConventions;
        RepoKnowledgeEntity entity = repository
                .findByRepoFullName(repoFullName)
                .orElseGet(() -> {
                    RepoKnowledgeEntity e = new RepoKnowledgeEntity();
                    e.setRepoFullName(repoFullName);
                    e.setReviewCount(0);
                    return e;
                });

        entity.setConventions(finalConventions);
        entity.setReviewCount(entity.getReviewCount() + 1);
        entity.setLastUpdated(LocalDateTime.now());
        repository.save(entity);

        log.info("Updated knowledge for {} (total reviews: {})",
                repoFullName, entity.getReviewCount());
    }

    private String buildKnowledgeEntry(FinalReview review, SecurityFinding security, LogicFinding logic, CoverageFinding coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Review Note (").append(LocalDateTime.now().toLocalDate()).append(")\n");
        sb.append("Verdict: ").append(review.verdict()).append("\n\n");

        if (security != null && security.severity() != Severity.INFO) {
            sb.append("- **SECURITY** in `").append(security.filePath()).append("`: ")
                    .append(security.description()).append("\n");
        }
        if (logic != null && logic.severity() != Severity.INFO) {
            sb.append("- **LOGIC** in `").append(logic.filePath()).append("`: ")
                    .append(logic.description()).append("\n");
        }
        if (coverage != null && coverage.severity() != Severity.INFO) {
            sb.append("- **COVERAGE** in `").append(coverage.filePath()).append("`: ")
                    .append(coverage.description()).append("\n");
        }

        return sb.toString();
    }
}
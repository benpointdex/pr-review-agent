package com.henry.git_pr.repository;

import com.henry.git_pr.model.entity.PrReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrReviewRepository extends JpaRepository<PrReviewEntity, Long> {
    List<PrReviewEntity> findByRepoFullNameOrderByCreatedAtDesc(String repoFullName);
    List<PrReviewEntity> findTop20ByOrderByCreatedAtDesc();
    Optional<PrReviewEntity> findByRepoFullNameAndPrNumber(String repoFullName, int prNumber);
}

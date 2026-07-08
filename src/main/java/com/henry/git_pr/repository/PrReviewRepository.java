package com.henry.git_pr.repository;

import com.henry.git_pr.model.entity.PrReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrReviewRepository extends JpaRepository<PrReviewEntity, Long> {
    List<PrReviewEntity> findByRepoFullNameOrderByCreatedAtDesc(String repoFullName);
    List<PrReviewEntity> findTop20ByOrderByCreatedAtDesc();
}

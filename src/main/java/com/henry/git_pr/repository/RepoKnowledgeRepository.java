package com.henry.git_pr.repository;

import com.henry.git_pr.model.entity.RepoKnowledgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RepoKnowledgeRepository extends JpaRepository<RepoKnowledgeEntity, Long> {
    Optional<RepoKnowledgeEntity> findByRepoFullName(String repoFullName);
}

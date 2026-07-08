package com.henry.git_pr.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "repo_knowledge")
public class RepoKnowledgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_full_name", unique = true, nullable = false)
    private String repoFullName;

    @Column(columnDefinition = "TEXT")
    private String conventions = "";

    @Column(name = "review_count")
    private int reviewCount = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

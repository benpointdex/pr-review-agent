package com.henry.git_pr.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pr_reviews")
public class PrReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "pr_title", length = 1000)
    private String prTitle;

    @Column(name = "pr_author")
    private String prAuthor;

    @Column(nullable = false, length = 10)
    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "security_finding", columnDefinition = "jsonb")
    private String securityFinding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "logic_finding", columnDefinition = "jsonb")
    private String logicFinding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coverage_finding", columnDefinition = "jsonb")
    private String coverageFinding;

    @Column(name = "formatted_comment", columnDefinition = "TEXT")
    private String formattedComment;

    @Column(name = "github_comment_id")
    private Long githubCommentId;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

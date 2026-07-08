-- Stores every review the agent has posted
-- Used for audit trail and potentially for analytics later
CREATE TABLE pr_reviews (
    id                  BIGSERIAL PRIMARY KEY,
    repo_full_name      VARCHAR(255) NOT NULL,
    pr_number           INT NOT NULL,
    pr_title            VARCHAR(1000),
    pr_author           VARCHAR(255),
    verdict             VARCHAR(10) NOT NULL,     -- PASS, FLAG, or BLOCK
    summary             TEXT,                     -- 2-3 sentence overview
    security_finding    JSONB,                    -- JSON of ReviewFinding
    logic_finding       JSONB,                    -- JSON of ReviewFinding
    coverage_finding    JSONB,                    -- JSON of ReviewFinding
    formatted_comment   TEXT,                     -- the exact text posted to GitHub
    github_comment_id   BIGINT,                   -- ID of the comment we posted
    processing_time_ms  BIGINT,                   -- how long the review took
    created_at          TIMESTAMP DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_pr_reviews_repo ON pr_reviews(repo_full_name);
CREATE INDEX idx_pr_reviews_verdict ON pr_reviews(verdict);
CREATE INDEX idx_pr_reviews_created ON pr_reviews(created_at DESC);

-- Unique constraint: one review per PR (prevent duplicate reviews)
CREATE UNIQUE INDEX idx_pr_reviews_unique_pr 
  ON pr_reviews(repo_full_name, pr_number);

COMMENT ON TABLE pr_reviews IS 
  'Audit log of every PR review posted by the agent.';

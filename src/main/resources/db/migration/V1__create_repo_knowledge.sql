-- Stores what the agent has learned about each repo
-- One row per GitHub repo
CREATE TABLE repo_knowledge (
    id              BIGSERIAL PRIMARY KEY,
    repo_full_name  VARCHAR(255) UNIQUE NOT NULL,  -- e.g. "owner/repo-name"
    conventions     TEXT DEFAULT '',               -- markdown text of learned patterns
    review_count    INT DEFAULT 0,                 -- how many PRs we've reviewed
    last_updated    TIMESTAMP DEFAULT NOW(),
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Index so lookups by repo name are fast
CREATE INDEX idx_repo_knowledge_name ON repo_knowledge(repo_full_name);

-- Add a comment so future-you knows what this table is
COMMENT ON TABLE repo_knowledge IS 
  'Per-repository memory for the PR Review Agent. 
   Updated after every review to improve future reviews on the same codebase.';

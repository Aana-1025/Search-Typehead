CREATE TABLE search_queries (
    id BIGSERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    normalized_query TEXT NOT NULL UNIQUE,
    total_count BIGINT NOT NULL DEFAULT 0,
    recent_1h_count BIGINT NOT NULL DEFAULT 0,
    recent_24h_count BIGINT NOT NULL DEFAULT 0,
    recent_7d_count BIGINT NOT NULL DEFAULT 0,
    trend_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_searched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_queries_trend_score
    ON search_queries (trend_score DESC);

CREATE INDEX idx_search_queries_total_count
    ON search_queries (total_count DESC);

CREATE TABLE query_prefixes (
    id BIGSERIAL PRIMARY KEY,
    prefix TEXT NOT NULL,
    query_id BIGINT NOT NULL REFERENCES search_queries(id) ON DELETE CASCADE,
    query_text TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    total_count BIGINT NOT NULL,
    trend_score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_query_prefixes_prefix_query_id UNIQUE (prefix, query_id)
);

CREATE INDEX idx_query_prefixes_prefix_score
    ON query_prefixes (prefix, trend_score DESC);

CREATE INDEX idx_query_prefixes_prefix_count
    ON query_prefixes (prefix, total_count DESC);

CREATE TABLE query_activity_buckets (
    id BIGSERIAL PRIMARY KEY,
    query_id BIGINT NOT NULL REFERENCES search_queries(id) ON DELETE CASCADE,
    bucket_start TIMESTAMP NOT NULL,
    bucket_granularity TEXT NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_query_activity_buckets_query_time_granularity
        UNIQUE (query_id, bucket_start, bucket_granularity)
);

CREATE INDEX idx_query_activity_buckets_query_time
    ON query_activity_buckets (query_id, bucket_start DESC);

CREATE TABLE batch_flush_audit (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL UNIQUE,
    raw_event_count BIGINT NOT NULL,
    unique_query_count BIGINT NOT NULL,
    db_write_count BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    status TEXT NOT NULL
);

CREATE TABLE processed_kafka_offsets (
    topic TEXT NOT NULL,
    partition_id INT NOT NULL,
    offset_until BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (topic, partition_id)
);

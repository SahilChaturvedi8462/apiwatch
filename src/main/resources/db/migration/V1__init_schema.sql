-- =============================================================================
-- APIWatch - Initial Database Migration
-- Version: V1
-- Description: Create users, monitored_endpoints, and health_checks tables
-- =============================================================================

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- 2. Monitored Endpoints Table
CREATE TABLE monitored_endpoints (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    current_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_monitored_endpoints_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_monitored_endpoints_user_id ON monitored_endpoints(user_id);
CREATE INDEX idx_monitored_endpoints_enabled ON monitored_endpoints(enabled);

-- 3. Health Checks History Table
CREATE TABLE health_checks (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,
    status_code INTEGER,
    response_time_ms BIGINT,
    success BOOLEAN NOT NULL,
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message VARCHAR(1024),
    CONSTRAINT fk_health_checks_endpoint FOREIGN KEY (endpoint_id) REFERENCES monitored_endpoints(id) ON DELETE CASCADE
);

CREATE INDEX idx_health_checks_endpoint_id ON health_checks(endpoint_id);
CREATE INDEX idx_health_checks_checked_at ON health_checks(checked_at DESC);
CREATE INDEX idx_health_checks_endpoint_checked ON health_checks(endpoint_id, checked_at DESC);

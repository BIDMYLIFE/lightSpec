CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL,
    normalized_username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
    created_at VARCHAR(40) NOT NULL
);

CREATE UNIQUE INDEX uk_users_normalized_username
    ON users (normalized_username);

CREATE INDEX idx_users_role
    ON users (role);

CREATE TABLE bootstrap_guard (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    version INTEGER NOT NULL DEFAULT 0
);

INSERT INTO bootstrap_guard (id, version) VALUES (1, 0);

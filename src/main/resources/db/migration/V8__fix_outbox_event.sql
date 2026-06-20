DROP TABLE outbox_event;

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    type VARCHAR(255),
    payload TEXT,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);
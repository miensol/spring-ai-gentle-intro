CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE support_tickets (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    customer_message TEXT NOT NULL,
    agent_response TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    embedding vector(1024)
);

CREATE INDEX ON support_tickets USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

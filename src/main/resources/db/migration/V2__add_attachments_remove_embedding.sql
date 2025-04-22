CREATE TABLE ticket_attachments (
    id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    content TEXT,
    binary_content BYTEA,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);
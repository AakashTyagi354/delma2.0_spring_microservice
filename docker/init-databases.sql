CREATE DATABASE delma_user_db;
CREATE DATABASE delma_doctor_db;
CREATE DATABASE delma_appointment_db;
CREATE DATABASE delma_payment_db;
CREATE DATABASE delma_document_db;
CREATE DATABASE delma_notification_db;
CREATE DATABASE delma_product_db;
CREATE DATABASE delma_category_db;
CREATE DATABASE delma_order_db;
CREATE DATABASE delma_ai_db;

\c delma_ai_db
CREATE EXTENSION IF NOT EXISTS vector;

-- embeddings table
-- stores each chunk of text + its vector representation
CREATE TABLE document_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id BIGINT NOT NULL,        -- which document this chunk belongs to
    user_id     VARCHAR NOT NULL,       -- which patient owns this document
    chunk_index INTEGER NOT NULL,       -- order of chunk within document
    chunk_text  TEXT NOT NULL,          -- actual text content of the chunk
    embedding   vector(1536),           -- 1536-dimension vector (Groq embedding size)
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON document_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
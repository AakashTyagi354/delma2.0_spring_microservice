package com.delma.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// DocumentEmbedding
//
// JPA entity for the document_embeddings table in delma_ai_db.
// Each row = one chunk of text + its vector representation.
//
// The 'embedding' field stores float[] mapped to pgvector's vector type.
// ─────────────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "document_embeddings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;


    // Just keep it simple — embedding field won't be used for insert
// JdbcTemplate handles the insert directly

    @Column(name = "embedding", columnDefinition = "vector(512)")
    private float[] embedding;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
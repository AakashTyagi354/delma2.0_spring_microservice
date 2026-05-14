package com.delma.aiservice.repository;


import com.delma.aiservice.entity.DocumentEmbedding;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentEmbeddingRepository
        extends JpaRepository<DocumentEmbedding, UUID> {
    // Find top K most relevant chunks for a user using cosine distance
    @Query(value = """
        SELECT chunk_text
        FROM document_embeddings
        WHERE user_id = :userId
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :k
        """, nativeQuery = true)
    List<String> findTopKChunksByUserId(
            @Param("userId") String userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("k") int k
    );
    void deleteByDocumentId(Long documentId);

}

package com.delma.aiservice.rag;


import com.delma.aiservice.entity.DocumentEmbedding;
import com.delma.aiservice.kafka.DocumentUploadedEvent;
import com.delma.aiservice.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexingService {

    private final S3Client s3Client;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public void indexDocument(DocumentUploadedEvent event) throws Exception{
        log.info("Starting RAG indexing for documentId: {}", event.getDocumentId());
        // 1. download pdf from s3

        byte[] pdfBytes = downloadFromS3(event.getS3Key());
        log.info("Downloaded PDF from S3: {} bytes", pdfBytes.length);

        // 2. Extract text
        String text = pdfTextExtractor.extract(pdfBytes);
        if(text.isBlank()){
            log.warn("No text extracted from documentId: {}. Skipping.",
                    event.getDocumentId());
            return;
        }

        // 3. split into chunks
        List<String> chunks = textChunker.chunk(text);
        log.info("Split into {} chunks for documentId: {}",
                chunks.size(), event.getDocumentId());

        // Step 4 + 5 — Embed each chunk and save to pgvector
        // Delete existing embeddings for this document first
        // (handles re-upload of same document)

        for(int i = 0;i< chunks.size();i++){
            String chunk = chunks.get(i);

            // Generate embedding vector for this chunk
            float[] embedding = embeddingService.embed(chunk);

            // save to pgvector
            DocumentEmbedding documentEmbedding = DocumentEmbedding.builder()
                    .documentId(event.getDocumentId())
                    .userId(event.getUserId())
                    .chunkIndex(i)
                    .chunkText(chunk)
                    .embedding(embedding)
                    .build();

            embeddingRepository.save(documentEmbedding);

            log.debug("Indexed chunk {}/{} for documentId: {}",
                    i + 1, chunks.size(), event.getDocumentId());
        }

        log.info("RAG indexing complete for documentId: {}. {} chunks indexed.",
                event.getDocumentId(), chunks.size());
    }
    private byte[] downloadFromS3(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> response =
                s3Client.getObjectAsBytes(request);

        return response.asByteArray();
    }

}

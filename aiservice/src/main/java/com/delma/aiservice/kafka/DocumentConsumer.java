package com.delma.aiservice.kafka;


import com.delma.aiservice.rag.RagIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentConsumer {
    private final RagIndexingService ragIndexingService;

    @KafkaListener(
            topics = "document-uploaded",
            groupId = "aiservice-rag-group"
    )
    public void consume(DocumentUploadedEvent event){
        log.info("Received document-uploaded event for documentId: {}",
                event.getDocumentId());

        if(!"application/pdf".equals(event.getContentType())){
            log.info("Skipping non-PDF document: {}", event.getFileName());
            return;
        }

        try {
            ragIndexingService.indexDocument(event);
        } catch (Exception e) {
            log.error("Failed to index documentId: {}. Error: {}",
                    event.getDocumentId(), e.getMessage(), e);
            // Don't rethrow — let Kafka commit the offset
            // Failed indexing should not block other events
        }
    }
}

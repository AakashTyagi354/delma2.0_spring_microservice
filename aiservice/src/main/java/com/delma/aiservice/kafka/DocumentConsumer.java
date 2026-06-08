package com.delma.aiservice.kafka;


import com.delma.aiservice.rag.RagIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentConsumer {
    private final RagIndexingService ragIndexingService;

    @KafkaListener(
            topics = "document-uploaded",
            groupId = "aiservice-rag-group",
            containerFactory = "documentKafkaListenerContainerFactory"
    )
    public void consume(DocumentUploadedEvent event){
        log.info("Received document-uploaded event for documentId: {}",
                event.getDocumentId());

        if(!"application/pdf".equals(event.getContentType())){
            log.info("Skipping non-PDF document: {}", event.getFileName());
            return;
        }

            ragIndexingService.indexDocument(event);
        log.info("Successfully indexed documentId: {}", event.getDocumentId());
    }
    @KafkaListener(
            topics = "document-uploaded.DLT",
            groupId = "aiservice-rag-dlt-group"
    )
    public void handleDlt(
            DocumentUploadedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "kafka_dlt-exception-fqcn",
                    required = false) String exceptionClass,
            @Header(name = "kafka_dlt-exception-message",
                    required = false) String errorMessage
    ) {
        log.error(
                "DLT — document indexing failed all retries | " +
                        "documentId: {} | fileName: {} | exception: {} | reason: {}",
                event.getDocumentId(),
                event.getFileName(),
                exceptionClass,
                errorMessage
        );
        // TODO: Save to failed_documents table for manual replay
        // TODO: Alert engineering team
    }
}

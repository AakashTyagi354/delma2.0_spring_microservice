package com.delma.documentservice.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventProducer {

    private final KafkaTemplate<String,DocumentUploadedEvent> kafkaTemplate;

    public static final String TOPIC = "document-uploaded";

    public void publish(DocumentUploadedEvent event){
        log.info("Publishing document uploaded event for documentId: {}",
                event.getDocumentId());
        kafkaTemplate.send(TOPIC,event);
    }

}

package com.delma.notificationservice.kafka;


import com.delma.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;

    // If you let exception propagate (no try-catch):
    //   createFromEvent() throws NullPointerException
    //   → exception propagates out of consume()
    //   → Spring Kafka catches it (via DefaultErrorHandler)
    //   → DefaultErrorHandler: retry 1s → retry 2s → retry 4s → DLT
    //   → partition unblocked
    //   → message preserved in DLT
    @KafkaListener(
            topics = "notification-topic",
            groupId = "notification-group"
    )
    // Spring Boot uses "kafkaListenerContainerFactory" by default
    // So this listener automatically uses our DefaultErrorHandler
    public void consume(NotificationEvent event){
        log.info("Received notification for userId: {}, title: {}",
                event.getUserId(), event.getTitle());
        notificationService.createFromEvent(event);
        log.info("Notification saved successfully for userId: {}",
                event.getUserId());
    }

    @KafkaListener(
            topics = "notification-topic-dlt",
            groupId = "notification-dlt-group"
    )
    public void handleDlt(
            // Accept String instead of NotificationEvent
            // WHY? DLT messages may have failed DURING deserialization
            // If we try to deserialize them as NotificationEvent again,
            // we get the same MessageConversionException → infinite loop
            // Raw String is always safe — we just log the raw JSON
            @org.springframework.messaging.handler.annotation.Payload String rawMessage,

            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "kafka_dlt-exception-fqcn",
                    required = false) String exceptionClass,
            @Header(name = "kafka_dlt-exception-message",
                    required = false) String errorMessage,
            @Header(name = "kafka_dlt-original-topic",
                    required = false) String originalTopic
    ) {
        log.error(
                "DLT — notification failed all retries | " +
                        "rawMessage: {} | exception: {} | reason: {} | originalTopic: {}",
                rawMessage,
                exceptionClass,
                errorMessage,
                originalTopic
        );
    }
}

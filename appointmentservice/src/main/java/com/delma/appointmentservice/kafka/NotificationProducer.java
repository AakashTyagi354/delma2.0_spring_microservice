package com.delma.appointmentservice.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String,NotificationEvent> kafkaTemplate;

    public void send(NotificationEvent event){
        log.info("Sending notification event to Kafka for user: {}", event.getUserId());
        kafkaTemplate.send("notification-topic",event.getUserId(),event);
    }
}

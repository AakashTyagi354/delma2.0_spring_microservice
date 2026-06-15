package com.delma.appointmentservice.kafka;

import com.delma.appointmentservice.entity.OutboxEvent;
import com.delma.appointmentservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String,String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents(){
        List<OutboxEvent> pending =  outboxRepository.findUnpublished(PageRequest.of(0,100));
        for(OutboxEvent event : pending){
            try{
                kafkaTemplate.send(
                        event.getTopic(),
                        String.valueOf(event.getAggregateId()),
                        event.getPayload()
                ).get();
            event.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(event);

                log.debug("Published outbox event {} to topic {}",
                        event.getId(), event.getTopic());

            }catch (Exception e) {
                log.error("Failed to publish outbox event {}, will retry",
                        event.getId(), e);
                // Don't update publishedAt — will be picked up next cycle
            }
        }
    }
}

package com.delma.aiservice.kafka;


// ─────────────────────────────────────────────────────────────────────────────
// ConsultationNotesConsumer
//
// Listens to "consultation-notes-ready" topic.
// Published by appointmentservice when doctor clicks "End Call & Save".
//
// Why Kafka and not direct call?
// AI report generation takes 5-10 seconds (Groq API call).
// Doctor should not wait 10 seconds for "End Call" to complete.
// Kafka makes it async — doctor gets instant response,
// AI processes in background, patient gets report when ready.
//
// Error handling: swallow exceptions — never rethrow.
// Doctor's notes are already saved in DB.
// AI report is a bonus — if it fails, patient still has basic info.
// ─────────────────────────────────────────────────────────────────────────────


import com.delma.aiservice.service.ConsultationAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationNotesConsumer {
        private final ConsultationAiService consultationAiService;

        @KafkaListener(
                topics = "consultation-notes-ready",
                groupId = "aiservice-consultation-group",
                containerFactory = "consultationKafkaListenerContainerFactory"
        )
        public void consume(ConsultationNotesEvent event){
            log.info("Received consultation-notes-ready for notesId: {}",
                    event.getNotesId());
            try{
                consultationAiService.processNotes(event);

            }catch(Exception e){
                // Swallow — never block Kafka offset commit
                // Notes are already saved — AI is enhancement not blocker
                //If you throw the error, your service will retrying processing that exact same
                // orrupted message over and over again.
//                The Consequences: Because Kafka processes a partition sequentially, all subsequent patient summaries behind this one in the
//                queue are blocked. Your entire AI service freezes, waiting for a broken message to succeed.
                log.error("Failed to process consultation notes for notesId: {}. " +
                        "Error: {}", event.getNotesId(), e.getMessage());
            }
        }

}

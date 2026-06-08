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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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

                consultationAiService.processNotes(event);

            log.info("AI report generated for notesId: {}", event.getNotesId());
        }
    @KafkaListener(
            topics = "consultation-notes-ready.DLT",
            groupId = "aiservice-consultation-dlt-group"
    )
    public void handleDlt(
            ConsultationNotesEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "kafka_dlt-exception-fqcn",
                    required = false) String exceptionClass,
            @Header(name = "kafka_dlt-exception-message",
                    required = false) String errorMessage
    ) {
        log.error(
                "DLT — consultation AI report failed all retries | " +
                        "notesId: {} | appointmentId: {} | exception: {} | reason: {}",
                event.getNotesId(),
                event.getAppointmentId(),
                exceptionClass,
                errorMessage
        );
        // TODO: Notify patient their AI report is delayed
        // TODO: Save to failed_consultations table for manual replay
    }
}

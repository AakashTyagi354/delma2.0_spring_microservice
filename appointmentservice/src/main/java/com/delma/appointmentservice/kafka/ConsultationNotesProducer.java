package com.delma.appointmentservice.kafka;


import com.delma.appointmentservice.entity.ConsultationNotes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationNotesProducer {
    private final KafkaTemplate<String,ConsultationNotesEvent> kafkaTemplate;
    private static final String TOPIC = "consultation-notes-ready";

    public void publishNotesReady(ConsultationNotes notes) {
        ConsultationNotesEvent event = ConsultationNotesEvent.builder()
                .notesId(notes.getId())
                .appointmentId(notes.getAppointmentId())
                .patientId(notes.getPatientId())
                .doctorId(notes.getDoctorId())
                .chiefComplaint(notes.getChiefComplaint())
                .diagnosis(notes.getDiagnosis())
                .diagnosisCode(notes.getDiagnosisCode())
                .vitals(notes.getVitals())
                .medications(notes.getMedications())
                .labTests(notes.getLabTests())
                .instructions(notes.getInstructions())
                .followUpDays(notes.getFollowUpDays())
                .build();

        kafkaTemplate.send(TOPIC, String.valueOf(notes.getId()), event);
        log.info("Published consultation-notes-ready for notesId: {}",
                notes.getId());
    }


}

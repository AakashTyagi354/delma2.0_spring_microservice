package com.delma.appointmentservice.kafka;


import com.delma.appointmentservice.dto.MedicationRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationNotesProducer {
    private Long notesId;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String chiefComplaint;
    private String diagnosis;
    private String diagnosisCode;
    private String vitals;
    private List<MedicationRecord> medications;
    private String labTests;
    private String instructions;
    private Integer followUpDays;
}

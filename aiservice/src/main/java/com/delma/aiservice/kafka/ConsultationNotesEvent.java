package com.delma.aiservice.kafka;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationNotesEvent {
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

    // Nested class — mirrors MedicationRecord in appointmentservice
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationRecord {
        private String drugName;
        private String genericName;
        private String dose;
        private String frequency;
        private Integer durationDays;
        private String route;
        private String notes;
    }
}

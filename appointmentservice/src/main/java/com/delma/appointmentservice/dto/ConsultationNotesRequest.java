package com.delma.appointmentservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationNotesRequest {
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private String chiefComplaint;
    private String diagnosis;
    private String diagnosisCode;
    private String vitals;
    private List<MedicationRecord> medications;
    private String labTests;
    private String instructions;
    private Integer followUpDays;
}
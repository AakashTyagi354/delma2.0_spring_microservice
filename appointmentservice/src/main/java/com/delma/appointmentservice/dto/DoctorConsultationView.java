package com.delma.appointmentservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DoctorConsultationView {
    private Long id;
    private Long appointmentId;
    private Long patientId;
    private String chiefComplaint;
    private String diagnosis;
    private String diagnosisCode;
    private String vitals;
    private List<MedicationRecord> medications;
    private String labTests;
    private String instructions;
    private Integer followUpDays;
    private String status;
    private LocalDateTime createdAt;
}
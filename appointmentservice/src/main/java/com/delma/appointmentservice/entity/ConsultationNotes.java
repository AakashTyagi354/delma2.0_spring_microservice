package com.delma.appointmentservice.entity;


import com.delma.appointmentservice.dto.MedicationRecord;
import com.delma.appointmentservice.utility.ConsultationStatus;
import com.delma.appointmentservice.utility.MedicationListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultation_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationNotes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "doctor_id", nullable = false)  // ← add this
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    // ICD-10 code e.g. "I20.0" for Unstable Angina
    @Column(name = "diagnosis_code")
    private String diagnosisCode;

    // e.g. "BP 145/90, HR 92, SpO2 96%, Temp 98.6F"
    @Column(name = "vitals", columnDefinition = "TEXT")
    private String vitals;

    // Stored as JSON array — see MedicationListConverter
    @Column(name = "medications_json", columnDefinition = "TEXT")
    @Convert(converter = MedicationListConverter.class)
    private List<MedicationRecord> medications = new ArrayList<>();

    // e.g. "ECG, CBC, Troponin, Chest X-ray"
    @Column(name = "lab_tests", columnDefinition = "TEXT")
    private String labTests;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "follow_up_days")
    private Integer followUpDays;

    @Column(name = "ai_report", columnDefinition = "TEXT")
    private String aiReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ConsultationStatus status;


    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ConsultationStatus.DRAFT;
        if (medications == null) medications = new ArrayList<>();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }





}

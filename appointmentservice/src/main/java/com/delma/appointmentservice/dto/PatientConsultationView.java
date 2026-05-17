package com.delma.appointmentservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PatientConsultationView {
    private Long id;
    private Long appointmentId;
    private String chiefComplaint;  // brief context only
    private String diagnosis;       // brief context only
    private String aiReport;        // full AI expanded report
    private String status;          // so frontend shows "generating..." vs "ready"
    private LocalDateTime createdAt;
}
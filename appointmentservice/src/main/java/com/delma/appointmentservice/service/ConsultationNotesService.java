package com.delma.appointmentservice.service;

import com.delma.appointmentservice.dto.ConsultationNotesRequest;
import com.delma.appointmentservice.dto.DoctorConsultationView;
import com.delma.appointmentservice.dto.PatientConsultationView;

import java.util.List;

public interface ConsultationNotesService {
    // Auto-save every 30s during call — creates or updates DRAFT
    DoctorConsultationView autoSave(ConsultationNotesRequest request);

    // Final save when doctor ends call — triggers AI via Kafka
    DoctorConsultationView saveFinal(ConsultationNotesRequest request);

    // Doctor reads all their patients' consultation history
    List<DoctorConsultationView> getDoctorConsultations(Long doctorId);

    // Doctor reads specific appointment notes
    DoctorConsultationView getDoctorView(Long appointmentId);

    // Patient reads all their consultation reports
    List<PatientConsultationView> getPatientConsultations(Long patientId);

    // Patient reads specific appointment report
    PatientConsultationView getPatientView(Long appointmentId);

    // Called by aiservice via Feign to save the AI report
    void updateAiReport(Long notesId, String aiReport);
}

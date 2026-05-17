package com.delma.appointmentservice.controller;


import com.delma.appointmentservice.dto.ConsultationNotesRequest;
import com.delma.appointmentservice.dto.DoctorConsultationView;
import com.delma.appointmentservice.dto.PatientConsultationView;
import com.delma.appointmentservice.service.ConsultationNotesService;
import com.delma.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/consultation-notes")
@RequiredArgsConstructor
public class ConsultationNotesController {
    private final ConsultationNotesService service;

    // ── Auto-save during call (every 30s from frontend) ───────────────────
    @PostMapping("/auto-save")
    public ResponseEntity<ApiResponse<DoctorConsultationView>> autoSave(
            @RequestBody ConsultationNotesRequest request) {
        log.info("Auto-save for appointmentId: {}", request.getAppointmentId());
        return ResponseEntity.ok(
                ApiResponse.success(service.autoSave(request), "Auto-saved"));
    }

    @PostMapping("/save-final")
    public ResponseEntity<ApiResponse<DoctorConsultationView>> saveFinal(
            @RequestBody ConsultationNotesRequest request) {
        log.info("Final save for appointmentId: {}", request.getAppointmentId());
        return ResponseEntity.ok(
                ApiResponse.success(service.saveFinal(request), "Notes saved"));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<DoctorConsultationView>>>
    getDoctorConsultations(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getDoctorConsultations(doctorId),
                "Doctor consultations fetched"));
    }

    @GetMapping("/appointment/{appointmentId}/doctor")
    public ResponseEntity<ApiResponse<DoctorConsultationView>>
    getDoctorView(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getDoctorView(appointmentId),
                "Consultation fetched"));
    }


    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientConsultationView>>>
    getPatientConsultations(@PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getPatientConsultations(patientId),
                "Patient consultations fetched"));
    }

    @GetMapping("/appointment/{appointmentId}/patient")
    public ResponseEntity<ApiResponse<PatientConsultationView>>
    getPatientView(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getPatientView(appointmentId),
                "Consultation fetched"));
    }

    @PutMapping("/{notesId}/ai-report")
    public ResponseEntity<ApiResponse<Void>> updateAiReport(
            @PathVariable Long notesId,
            @RequestBody String aiReport) {
        service.updateAiReport(notesId, aiReport);
        return ResponseEntity.ok(
                ApiResponse.success(null, "AI report saved"));
    }


}

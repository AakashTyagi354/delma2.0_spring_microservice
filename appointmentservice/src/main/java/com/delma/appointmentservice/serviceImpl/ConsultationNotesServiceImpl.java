package com.delma.appointmentservice.serviceImpl;

import com.delma.appointmentservice.dto.ConsultationNotesRequest;
import com.delma.appointmentservice.dto.DoctorConsultationView;
import com.delma.appointmentservice.dto.PatientConsultationView;
import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.ConsultationNotes;
import com.delma.appointmentservice.entity.OutboxEvent;
import com.delma.appointmentservice.kafka.ConsultationNotesEvent;
import com.delma.appointmentservice.kafka.ConsultationNotesProducer;
import com.delma.appointmentservice.repository.AppointmentRepository;
import com.delma.appointmentservice.repository.ConsultationNotesRepository;
import com.delma.appointmentservice.repository.OutboxRepository;
import com.delma.appointmentservice.service.ConsultationNotesService;
import com.delma.appointmentservice.utility.ConsultationStatus;
import com.delma.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationNotesServiceImpl implements ConsultationNotesService {

    private final ConsultationNotesRepository repository;
    private final ConsultationNotesProducer eventProducer;
    private final AppointmentRepository appointmentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorConsultationView autoSave(ConsultationNotesRequest request) {
        ConsultationNotes notes = findOrCreate(request);
        updateFields(notes, request);
        notes.setStatus(ConsultationStatus.DRAFT);
        ConsultationNotes saved = repository.save(notes);
        log.info("Auto-saved notes for appointmentId: {}", request.getAppointmentId());
        return toDoctorView(saved);
    }

    @Override
    @Transactional
    public DoctorConsultationView saveFinal(ConsultationNotesRequest request) {
        ConsultationNotes notes = findOrCreate(request);
        updateFields(notes, request);
        notes.setStatus(ConsultationStatus.SAVED);
        ConsultationNotes saved = repository.save(notes);

        ConsultationNotesEvent event = ConsultationNotesEvent.builder()
                .notesId(saved.getId())
                .appointmentId(saved.getAppointmentId())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .chiefComplaint(saved.getChiefComplaint())
                .diagnosis(saved.getDiagnosis())
                .diagnosisCode(saved.getDiagnosisCode())
                .vitals(saved.getVitals())
                .medications(saved.getMedications())
                .labTests(saved.getLabTests())
                .instructions(saved.getInstructions())
                .followUpDays(saved.getFollowUpDays())
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("ConsultationNotes")
                .aggregateId(saved.getId())
                .eventType("ConsultationNotesReady")
                .topic("consultation-notes-ready")
                .payload(serialize(event))
                .build();
        outboxRepository.save(outboxEvent);

        log.info("Final save done, AI event published for appointmentId: {}",
                request.getAppointmentId());
        return toDoctorView(saved);
    }

    @Override
    public List<DoctorConsultationView> getDoctorConsultations(Long doctorId) {
        return repository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream()
                .map(this::toDoctorView)
                .collect(Collectors.toList());
    }

    @Override
    public DoctorConsultationView getDoctorView(Long appointmentId) {
        ConsultationNotes notes = repository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notes not found for appointmentId: " + appointmentId));
        return toDoctorView(notes);
    }

    @Override
    public List<PatientConsultationView> getPatientConsultations(Long patientId) {
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toPatientView)
                .collect(Collectors.toList());
    }

    @Override
    public PatientConsultationView getPatientView(Long appointmentId) {
        ConsultationNotes notes = repository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notes not found for appointmentId: " + appointmentId));
        return toPatientView(notes);
    }

    @Override
    public void updateAiReport(Long notesId, String aiReport) {
        ConsultationNotes notes = repository.findById(notesId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notes not found: " + notesId));
        notes.setAiReport(aiReport);
        notes.setStatus(ConsultationStatus.AI_PROCESSED);
        repository.save(notes);
        log.info("AI report saved for notesId: {}", notesId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    // Upsert — find existing draft or create new one
    // Fetches doctorId and patientId from Appointment entity
    // Frontend only sends appointmentId — we derive the rest
    private ConsultationNotes findOrCreate(ConsultationNotesRequest request) {
        return repository
                .findByAppointmentId(request.getAppointmentId())
                .orElseGet(() -> {
                    Appointment appointment = appointmentRepository
                            .findById(request.getAppointmentId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Appointment not found: "
                                    + request.getAppointmentId()));
                    return ConsultationNotes.builder()
                            .appointmentId(request.getAppointmentId())
                            .doctorId(appointment.getDoctorId())
                            .patientId(appointment.getUserId())
                            .build();
                });
    }

    private void updateFields(ConsultationNotes notes,
                               ConsultationNotesRequest request) {
        notes.setChiefComplaint(request.getChiefComplaint());
        notes.setDiagnosis(request.getDiagnosis());
        notes.setDiagnosisCode(request.getDiagnosisCode());
        notes.setVitals(request.getVitals());
        notes.setLabTests(request.getLabTests());
        notes.setInstructions(request.getInstructions());
        notes.setFollowUpDays(request.getFollowUpDays());
        notes.setMedications(
                request.getMedications() != null
                        ? request.getMedications()
                        : new ArrayList<>()
        );
    }

    private DoctorConsultationView toDoctorView(ConsultationNotes n) {
        return DoctorConsultationView.builder()
                .id(n.getId())
                .appointmentId(n.getAppointmentId())
                .patientId(n.getPatientId())
                .chiefComplaint(n.getChiefComplaint())
                .diagnosis(n.getDiagnosis())
                .diagnosisCode(n.getDiagnosisCode())
                .vitals(n.getVitals())
                .medications(n.getMedications())
                .labTests(n.getLabTests())
                .instructions(n.getInstructions())
                .followUpDays(n.getFollowUpDays())
                .status(n.getStatus().name())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private PatientConsultationView toPatientView(ConsultationNotes n) {
        return PatientConsultationView.builder()
                .id(n.getId())
                .appointmentId(n.getAppointmentId())
                .chiefComplaint(n.getChiefComplaint())
                .diagnosis(n.getDiagnosis())
                .aiReport(n.getAiReport())
                .status(n.getStatus().name())
                .createdAt(n.getCreatedAt())
                .build();
    }
    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}

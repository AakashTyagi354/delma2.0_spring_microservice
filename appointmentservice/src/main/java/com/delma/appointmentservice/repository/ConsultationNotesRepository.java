package com.delma.appointmentservice.repository;

import com.delma.appointmentservice.entity.ConsultationNotes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationNotesRepository extends JpaRepository<ConsultationNotes,Long> {
    Optional<ConsultationNotes> findByAppointmentId(Long appointmentId);
    List<ConsultationNotes> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    List<ConsultationNotes> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}

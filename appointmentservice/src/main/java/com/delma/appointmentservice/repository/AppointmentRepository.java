package com.delma.appointmentservice.repository;


import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.utility.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Long> {

    Boolean existsByDoctorIdAndSlotIdAndStatus(Long doctorId, Long slotId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    // Optional: Get appointments for a patient
    List<Appointment> findByUserId(Long userId);


}

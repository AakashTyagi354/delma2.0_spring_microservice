package com.delma.appointmentservice.service;

import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    public Appointment bookAppointment(Long userId, Long doctorId, Long slotId);
    public List<DoctorSlot> getAvailableSlots(Long doctorId, LocalDate date);
    public List<Appointment> getAppointmentsForUser(Long userId);

}

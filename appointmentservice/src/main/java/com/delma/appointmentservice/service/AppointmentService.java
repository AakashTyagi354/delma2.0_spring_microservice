package com.delma.appointmentservice.service;

import com.delma.appointmentservice.dto.AppointmentResponse;
import com.delma.appointmentservice.dto.DoctorSlotResponse;
import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.utility.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
      AppointmentResponse bookAppointment(Long userId, Long doctorId, Long slotId);
      List<DoctorSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);
      List<AppointmentResponse> getAppointmentsForUser(Long userId);
      String getMeetingToken(Long appointmentId, String userId, String rolesHeaders);
     List<AppointmentResponse> getAppointmentForDoctors(Long doctorId);

}

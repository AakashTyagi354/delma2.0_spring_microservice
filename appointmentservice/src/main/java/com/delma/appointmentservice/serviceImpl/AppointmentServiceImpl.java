package com.delma.appointmentservice.serviceImpl;

import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.repository.AppointmentRepository;
import com.delma.appointmentservice.repository.DoctorSlotRepository;
import com.delma.appointmentservice.service.AppointmentService;
import com.delma.appointmentservice.utility.AppointmentStatus;
import com.delma.appointmentservice.utility.SlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository slotRepository;

    public Appointment bookAppointment(Long userId,Long doctorId,Long slotId){
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        Boolean isBooked = appointmentRepository.existsByDoctorIdAndSlotIdAndStatus(doctorId,slotId, AppointmentStatus.BOOKED);
        if(isBooked || slot.getStatus().equals(SlotStatus.BOOKED)) throw new RuntimeException("Slot already booked");

        Appointment appointment = Appointment.builder()
                .doctorId(doctorId)
                .userId(userId)
                .slotId(slotId)
                .status(AppointmentStatus.BOOKED)
                .createdAt(LocalDateTime.now())
                .build();

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        return appointmentRepository.save(appointment);
    }


    public List<DoctorSlot> getAvailableSlots(Long doctorId, LocalDate date) {
        return slotRepository.findByDoctorIdAndDateAndStatus(doctorId, date, SlotStatus.AVAILABLE);
    }

    public List<Appointment> getAppointmentsForUser(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

}

package com.delma.appointmentservice.serviceImpl;

import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.kafka.NotificationEvent;
import com.delma.appointmentservice.kafka.NotificationProducer;
import com.delma.appointmentservice.repository.AppointmentRepository;
import com.delma.appointmentservice.repository.DoctorSlotRepository;
import com.delma.appointmentservice.response.ApiResponse;
import com.delma.appointmentservice.service.AppointmentService;
import com.delma.appointmentservice.utility.AppointmentStatus;
import com.delma.appointmentservice.utility.SlotStatus;
import com.delma.appointmentservice.utility.ZegoTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository slotRepository;
    private final NotificationProducer notificationProducer;

    @Value("${zego.app.id}") private long appId;
    @Value("${zego.server.secret}") private String serverSecret;

    public Appointment bookAppointment(Long userId, Long doctorId, Long slotId){
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

        Appointment bookedAppointmnet = appointmentRepository.save(appointment);
        NotificationEvent event = new NotificationEvent(
                bookedAppointmnet.getUserId().toString(),
                "Appointment Booked Successfully",
                "Congratulations! Your appointment has been booked successfully with the doctor.",
                "Appointment Service"
        );
        notificationProducer.send(event);

        return bookedAppointmnet;
    }


    public List<DoctorSlot> getAvailableSlots(Long doctorId, LocalDate date) {
        log.info("Fetching available slots for doctorId: {} on date: {}", doctorId, date);
        return slotRepository.findByDoctorIdAndDateAndStatus(doctorId, date, SlotStatus.AVAILABLE);
    }

    public List<Appointment> getAppointmentsForUser(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }


    @Override
    public String getMeetingToken(Long appointmentId, String userId, String rolesHeaders) {
        log.info("Inside getMeethingToken userId: {} appoitmentId: {}, roles: {}",userId,appointmentId,rolesHeaders);
        List<String> roles = Arrays.asList(rolesHeaders.split(","));
        Appointment appt = appointmentRepository.findById(appointmentId).orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        boolean isAuthorized = false;
//        if(roles.contains("DOCTOR") && !appt.getDoctorId().equals(userId)){
//            throw new RuntimeException("Unauthorized: User does not have access to this appointment.");
//        }else (roles.contains("USER") && !appt.getUserId().equals(userId)){
//            throw new RuntimeException("Unauthorized: User does not have access to this appointment.");
//        }



        try{
            // Inside your Service method
            log.info("aapID: {} and secret: {}",appId,serverSecret);
            String rawToken04 = ZegoTokenUtils.generateToken04(appId, userId, serverSecret, 3600, String.valueOf(appointmentId));

// This is what you actually return to your Frontend
            String finalTokenForFrontend = ZegoTokenUtils.makeKitToken(appId, String.valueOf(appointmentId), rawToken04);

            return finalTokenForFrontend;


        }catch (Exception e){
            throw new RuntimeException("Error generating video token");
        }


    }

    @Override
    public List<Appointment> getAppointmentForDoctors(Long doctorId) {
        return appointmentRepository.findByDoctorIdAndStatus(doctorId,AppointmentStatus.BOOKED);
    }


}

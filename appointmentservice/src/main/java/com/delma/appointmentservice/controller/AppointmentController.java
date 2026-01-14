package com.delma.appointmentservice.controller;

import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.response.ApiResponse;
import com.delma.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping("/slots")
    public List<DoctorSlot> getAvailableSlots(@RequestParam Long doctorId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Received request for available slots for doctorId: {} on date: {}", doctorId, date);
        return appointmentService.getAvailableSlots(doctorId, date);
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Appointment>> bookAppointment(@RequestParam Long userId,
                                                                    @RequestParam Long doctorId,
                                                                    @RequestParam Long slotId) {
        Appointment app = appointmentService.bookAppointment(userId, doctorId, slotId);
        return ResponseEntity.ok(
                ApiResponse.success(app, "Appointment booked successfully")
        );
    }

    @GetMapping("/user")
    public List<Appointment> getAppointmentsForUser(@RequestParam Long userId) {
        return appointmentService.getAppointmentsForUser(userId);
    }

    @GetMapping("/doctor")
    public List<Appointment> getAppointmentsForDoctore(@RequestParam Long doctorId) {
        return appointmentService.getAppointmentForDoctors(doctorId);
    }

    @GetMapping("/video-token/{appointmentId}")
    public ResponseEntity<?> getToken(
            @PathVariable Long appointmentId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Roles") String roles) {

        String token = appointmentService.getMeetingToken(appointmentId, userId, roles);
        return ResponseEntity.ok(Map.of("token", token));
    }


}

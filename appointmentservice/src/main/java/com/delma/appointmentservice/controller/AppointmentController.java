package com.delma.appointmentservice.controller;

import com.delma.appointmentservice.dto.AppointmentResponse;
import com.delma.appointmentservice.dto.DoctorSlotResponse;
import com.delma.appointmentservice.service.AppointmentService;
import com.delma.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<DoctorSlotResponse>>> getAvailableSlots(@RequestParam Long doctorId,
                                                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAvailableSlots(doctorId, date),"All the slots as requested my provided Filters"));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(@RequestParam Long userId,
                                                                            @RequestParam Long doctorId,
                                                                            @RequestParam Long slotId) {
        AppointmentResponse app = appointmentService.bookAppointment(userId, doctorId, slotId);
        return ResponseEntity.ok(
                ApiResponse.success(app, "Appointment booked successfully")
        );
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsForUser(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForUser(userId),"Getting appointment for a User"));
    }

    @GetMapping("/doctor")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsForDoctore(@RequestParam Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentForDoctors(doctorId),"Getting appointment for a doctor"));
    }

    @GetMapping("/video-token/{appointmentId}")
    public ResponseEntity<ApiResponse<String>> getToken(
            @PathVariable Long appointmentId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Roles") String roles) {

        String token = appointmentService.getMeetingToken(appointmentId, userId, roles);
        return ResponseEntity.ok(ApiResponse.success(token,"Token for appointment"));
    }


}

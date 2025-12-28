package com.delma.appointmentservice.controller;

import com.delma.appointmentservice.entity.Appointment;
import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
        private final AppointmentService appointmentService;

        @GetMapping("/slots")
        public List<DoctorSlot> getAvailableSlots(@RequestParam Long doctorId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
                log.info("Received request for available slots for doctorId: {} on date: {}", doctorId, date);
                return appointmentService.getAvailableSlots(doctorId,date);
        }

        @PostMapping("/book")
        public Appointment bookAppointment(@RequestParam Long userId,
                                           @RequestParam Long doctorId,
                                           @RequestParam Long slotId){
                return appointmentService.bookAppointment(userId,doctorId,slotId);
        }

        @GetMapping("/user")
        public List<Appointment> getAppointmentsForUser(@RequestParam Long userId){
                return appointmentService.getAppointmentsForUser(userId);
        }

}

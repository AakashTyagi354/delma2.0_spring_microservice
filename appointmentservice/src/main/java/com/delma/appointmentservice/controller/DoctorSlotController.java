package com.delma.appointmentservice.controller;


import com.delma.appointmentservice.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class DoctorSlotController {
    private final SlotService slotService;

    @PostMapping("/create")
    public ResponseEntity<String> createSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam int slotDurationMinutes
    ) {
        slotService.createSlots(doctorId, date, startTime, endTime, slotDurationMinutes);
        return ResponseEntity.ok("Slots created successfully");

    }
}

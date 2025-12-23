package com.delma.doctorservice.controller;

import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.entity.Doctor;
import com.delma.doctorservice.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping("/apply")
    public ResponseEntity<String> applyDoctor(@RequestParam String userId,
                                              @RequestBody DoctorApplicationRequest request) {
        System.out.println("Received application for userId: " + userId);
        doctorService.submitApplication(userId, request);
        return ResponseEntity.ok("Application submitted successfully");
    }

    // Admin endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approve(@PathVariable Long id) {
        doctorService.approveApplication(id);
        return ResponseEntity.ok("Application approved");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject/{id}")
    public ResponseEntity<String> reject(@PathVariable Long id) {
        System.out.println("Rejecting application with id: " + id);
        doctorService.rejectApplication(id);
        return ResponseEntity.ok("Application rejected");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<Doctor>> pendingApplications() {
        return ResponseEntity.ok(doctorService.getPendingApplications());
    }

    @GetMapping("/all")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        log.info("Fetching all approved doctors");
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }
}

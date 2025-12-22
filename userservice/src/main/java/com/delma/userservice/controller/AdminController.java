package com.delma.userservice.controller;

import com.delma.userservice.client.DoctorClient;
import com.delma.userservice.dto.DoctorResponseDTO;
import com.delma.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final DoctorClient doctorClient;
    private final HttpServletRequest request;
    private final UserService userService;

    @PutMapping("/approve-doctors/{doctorId}")
    public ResponseEntity<Void> approveDoctor(@PathVariable String doctorId) {
        // Extract JWT token from incoming request
        String token = request.getHeader("Authorization");

        // Forward token to Doctor MS
        doctorClient.approveDoctor(doctorId, token);

        return ResponseEntity.ok().build();
    }



    // Reject doctor application
    @PostMapping("/reject-doctors/{doctorId}")
    public ResponseEntity<String> rejectDoctor(@PathVariable String doctorId) {
        String token = request.getHeader("Authorization");
        String res = doctorClient.rejectApplication(Long.valueOf(doctorId), token);
        return ResponseEntity.ok(res);
    }

    // Get all pending doctor applications
    @GetMapping("/pending-doctors")
    public ResponseEntity<List<DoctorResponseDTO>> getPendingDoctors() {
        String token = request.getHeader("Authorization");
        List<DoctorResponseDTO> pending = doctorClient.getPendingApplications(token);
        return ResponseEntity.ok(pending);
    }

    // Add doctor role to a user
    @PutMapping("/add-role/doctor/{userId}")
    public ResponseEntity<Void> addDoctorRole(@PathVariable Long userId) {
        userService.addRoleDoctor(userId);
        return ResponseEntity.ok().build();
    }



}

package com.delma.userservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.userservice.client.DoctorClient;
import com.delma.userservice.dto.DoctorResponseDTO;
import com.delma.userservice.dto.UserResponse;


import com.delma.userservice.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
//@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final DoctorClient doctorClient;
    private final HttpServletRequest request;
    private final UserService userService;

    @CircuitBreaker(name = "doctorservice")
    @PutMapping("/approve-doctors/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> approveDoctor(@PathVariable String doctorId) {
        // Extract JWT token from incoming request


        // Forward token to Doctor MS
        doctorClient.approveDoctor(doctorId, request.getHeader("Authorization"));

        return ResponseEntity.ok(
                ApiResponse.success("Doctor application approved successfully")
        );
    }



    // Reject doctor application
    @PutMapping("/reject-doctors/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> rejectDoctor(@PathVariable String doctorId) {
//        String token = request.getHeader("Authorization");
        ApiResponse<Void> res = doctorClient.rejectApplication(Long.valueOf(doctorId));
        return ResponseEntity.ok(
                ApiResponse.success(res.getData(), "Doctor application rejected successfully")
        );
    }

    // Get all pending doctor applications
    @GetMapping("/pending-doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponseDTO>>> getPendingDoctors() {
//        String token = request.getHeader("Authorization");
        List<DoctorResponseDTO> pending = doctorClient.getPendingApplications().getData();
        return ResponseEntity.ok(
                ApiResponse.success(pending, "Fetched pending doctor applications successfully")
        );
    }

    // Add doctor role to a user
    @PutMapping("/add-role/doctor/{userId}")
    public ResponseEntity<ApiResponse<Void>> addDoctorRole(@PathVariable Long userId) {
        userService.addRoleDoctor(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Doctor role added to user successfully")
        );
    }

    @GetMapping("/getall-users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(){
            List<UserResponse> allUsers = userService.findAllUsers();

            return ResponseEntity.ok(
                    ApiResponse.success(allUsers,"Fetching all the users from admin portal")
            );
    }





}

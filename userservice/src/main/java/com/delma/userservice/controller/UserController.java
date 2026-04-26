package com.delma.userservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.userservice.client.DoctorClient;
import com.delma.userservice.dto.DoctorResponseDTO;
import com.delma.userservice.dto.UserResponse;
import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;

import com.delma.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final DoctorClient doctorClient;
    private final UserService userService;
    private final HttpServletRequest request;


    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody User user) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(user),"User Created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id),"Getting single user for the provided userId"));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/apply-doctor")
    public ResponseEntity<ApiResponse<Void>> applyDoctor(@RequestBody Doctor request, Authentication authentication){
        String email = authentication.getName();
        userService.applyDoctor(request, email);
        return ResponseEntity.ok(ApiResponse.success("Doctor application submitted successfully"));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponseDTO>>> getAllDoctors(){
        List<DoctorResponseDTO> allApprovedDoctors = doctorClient.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success(allApprovedDoctors,"Getting all approved doctors"));
    }
}
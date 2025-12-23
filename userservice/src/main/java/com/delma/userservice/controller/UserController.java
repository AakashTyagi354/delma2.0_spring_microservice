package com.delma.userservice.controller;

import com.delma.userservice.client.DoctorClient;
import com.delma.userservice.dto.DoctorResponseDTO;
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

    @GetMapping("/test")
    public String test(Authentication auth) {
        System.out.println("Authenticated: {}"+ auth);
        return "ok";
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/apply-doctor")
    public ResponseEntity<String> applyDoctor(@RequestBody Doctor request, Authentication authentication){
        String email = authentication.getName();
        userService.applyDoctor(request, email);
        return ResponseEntity.ok("Doctor application submitted successfully");
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponseDTO>> getAllDoctors(){
        log.info("Fetching all approved doctors");
        String token = request.getHeader("Authorization");
        List<DoctorResponseDTO> allApprovedDoctors = doctorClient.getAllDoctors(token);
        return ResponseEntity.ok(allApprovedDoctors);
    }
}
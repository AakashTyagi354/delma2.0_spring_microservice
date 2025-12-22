package com.delma.userservice.controller;

import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;
import com.delma.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
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
}
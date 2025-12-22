package com.delma.userservice.controller;

import com.delma.userservice.dto.LoginRequestDTO;
import com.delma.userservice.dto.LoginResponseDTO;
import com.delma.userservice.dto.SignupResponseDTO;
import com.delma.userservice.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        // Implement login logic here

        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDTO> signUp(@RequestBody LoginRequestDTO signupRequestDTO) {
        // Implement login logic here

        return ResponseEntity.ok(authService.signup(signupRequestDTO));
    }

    @PostMapping("/admin-login")
    public ResponseEntity<LoginResponseDTO> adminLogin(@RequestBody LoginRequestDTO loginRequest) throws AccessDeniedException {
        // Implement admin login logic here

        return ResponseEntity.ok(authService.adminLogin(loginRequest));
    }

}

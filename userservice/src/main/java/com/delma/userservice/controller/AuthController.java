package com.delma.userservice.controller;

import com.delma.userservice.dto.LoginRequestDTO;
import com.delma.userservice.dto.LoginResponseDTO;
import com.delma.userservice.dto.SignupResponseDTO;
import com.delma.userservice.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequest) {
        // Implement login logic here

        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.login(loginRequest),
                        "Login successful"
                )
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDTO>> signUp(@RequestBody LoginRequestDTO signupRequestDTO) {
        // Implement login logic here

        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.signup(signupRequestDTO),
                        "Signup successful"
                )
        );
    }

    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> adminLogin(@RequestBody LoginRequestDTO loginRequest) throws AccessDeniedException {
        // Implement admin login logic here

        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.adminLogin(loginRequest),
                        "Admin login successful"
                )
        );
    }

}

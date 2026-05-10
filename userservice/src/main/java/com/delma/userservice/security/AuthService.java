package com.delma.userservice.security;

import com.delma.common.exception.BadRequestException;
import com.delma.common.exception.ResourceNotFoundException;
import com.delma.userservice.Enum.Role;
import com.delma.userservice.config.AppConfig;
import com.delma.userservice.dto.*;
import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import com.delma.userservice.service.EmailService;
import com.delma.userservice.service.OtpService;
import com.delma.userservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserReposistory userReposistory;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final EmailService emailService;

    public  LoginResponseDTO login(LoginRequestDTO loginRequest) {

//        Login Request
//   ↓
//        AuthenticationManager
//   ↓
//        DaoAuthenticationProvider
//   ↓
//        UserDetailsService → DB
//   ↓
//        PasswordEncoder → compare
//   ↓
//        Authenticated User
        log.info("Attempting to authenticate user with email: {}", loginRequest.getEmail());


        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword())
        );
//        User user = (User) authentication.getPrincipal();
//        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.info("User {} authenticated successfully", userDetails.getUsername());
        User user = userReposistory
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for id: "+userDetails.getUsername()));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Email not verified. Please verify your email before logging in.");
        }
        // Here you would typically generate a JWT or session token
        log.info("user: {}",user);
        String token = authUtil.generateAccessToken(user);
        log.info("Access token generated for user: {}", loginRequest.getEmail());
        // Refresh token generation
        String refreshToken = UUID.randomUUID().toString();
        log.info("user ID: {}", user.getId());
        refreshTokenService.save(refreshToken,user.getId(), Instant.now().plus(7, ChronoUnit.DAYS));
        log.info("User {} authenticated successfully", loginRequest.getEmail());

        String isAdmin = user.getIsAdmin().equals("true") ? "true" : "false";
        log.info("isAdmin flag for user {}: {}", loginRequest.getEmail(), isAdmin);
        return new LoginResponseDTO(token, user.getId(),user.getRoles().toString(), isAdmin,user.getName(),refreshToken);

    }

    // ---------------- SIGNUP ----------------
    public SignupResponseDTO signup(LoginRequestDTO signupRequestDTO) {

        if (userReposistory.findByEmail(signupRequestDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .name(signupRequestDTO.getName())
                .email(signupRequestDTO.getEmail()) // ⚠️ ideally use getEmail()
                .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                .isAdmin("false")
                .isDoctor("false")
                .isVerified(false)
                .roles(Set.of(Role.USER))
                .build();

        userReposistory.save(user);

        // generate OTP and send email
        String otp = otpService.generateAndStoreOtp((signupRequestDTO.getEmail()));
        emailService.sendOtpEmail(signupRequestDTO.getEmail(),otp);

        log.info("User registered successfully, OTP sent to: {}", signupRequestDTO.getEmail());
        return new SignupResponseDTO(user.getName(), user.getEmail());
    }

    public String verifyOtp(VerifyOtpRequest request){
        User user = userReposistory.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Email already verified.");
        }

        boolean isValid = otpService.verifyOtp(request.email(), request.otp());
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP. Please request a new one.");
        }
        user.setIsVerified(true);
        userReposistory.save(user);

        log.info("Email verified successfully for: {}", request.email());
        return "Email verified successfully. You can now log in.";
    }

    public String resendOtp(ResendOtpRequest request) {
        User user = userReposistory.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Email already verified.");
        }

        String otp = otpService.generateAndStoreOtp(request.email());
        emailService.sendOtpEmail(request.email(), otp);

        log.info("OTP resent to: {}", request.email());
        return "OTP resent successfully. Please check your email.";
    }

    public  LoginResponseDTO adminLogin(LoginRequestDTO loginRequest) throws AccessDeniedException {
        // 1️⃣ Authenticate first (email + password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );



        UserDetails userDetails =  (UserDetails) authentication.getPrincipal();
        User authenticatedUser = userReposistory
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + loginRequest.getEmail()));


        // 3️⃣ Check ADMIN role AFTER authentication
        if (!authenticatedUser.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Access Denied: Admin access required");
        }

        // 4️⃣ Generate JWT using authenticated user
        String token = authUtil.generateAccessToken(authenticatedUser);

        return new LoginResponseDTO(
                token,
                authenticatedUser.getId(),
                authenticatedUser.getRoles().toString(),
                "true",
                authenticatedUser.getName(),
                null
        );
    }
}

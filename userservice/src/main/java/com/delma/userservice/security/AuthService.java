package com.delma.userservice.security;

import com.delma.userservice.Enum.Role;
import com.delma.userservice.config.AppConfig;
import com.delma.userservice.dto.LoginRequestDTO;
import com.delma.userservice.dto.LoginResponseDTO;
import com.delma.userservice.dto.SignupResponseDTO;
import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
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
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Here you would typically generate a JWT or session token
        String token = authUtil.generateAccessToken(user);
        log.info("Access token generated for user: {}", loginRequest.getEmail());
        // Refresh token generation
        String refreshToken = UUID.randomUUID().toString();
        log.info("user ID: {}", user.getId());
        refreshTokenService.save(refreshToken,user.getId(), Instant.now().plus(7, ChronoUnit.DAYS));
        log.info("User {} authenticated successfully", loginRequest.getEmail());

        String isAdmin = user.getRoles().toString().equals("ADMIN") ? "true" : "false";
        log.info("isAdmin flag for user {}: {}", loginRequest.getEmail(), isAdmin);
        return new LoginResponseDTO(token, user.getId(),user.getRoles().toString(), isAdmin,user.getName(),refreshToken);

    }

    // ---------------- SIGNUP ----------------
    public SignupResponseDTO signup(LoginRequestDTO signupRequestDTO) {

        if (userReposistory.findByEmail(signupRequestDTO.getName()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .name(signupRequestDTO.getName())
                .email(signupRequestDTO.getEmail()) // ⚠️ ideally use getEmail()
                .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                .isAdmin("false")
                .isDoctor("false")
                .roles(Set.of(Role.USER))
                .build();

        userReposistory.save(user);

        return new SignupResponseDTO(user.getId(), user.getName(), user.getEmail());
    }

    public  LoginResponseDTO adminLogin(LoginRequestDTO loginRequest) throws AccessDeniedException {
        // 1️⃣ Authenticate first (email + password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // 2️⃣ Get authenticated principal
        User authenticatedUser = (User) authentication.getPrincipal();

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

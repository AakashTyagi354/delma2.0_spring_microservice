package com.delma.userservice.controller;

import com.delma.userservice.dto.LoginRequestDTO;
import com.delma.userservice.dto.LoginResponseDTO;
import com.delma.userservice.dto.SignupResponseDTO;
import com.delma.userservice.entity.RefreshToken;
import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import com.delma.userservice.response.ApiResponse;
import com.delma.userservice.response.AuthTokenResponse;
import com.delma.userservice.security.AuthService;
import com.delma.userservice.security.AuthUtil;
import com.delma.userservice.service.RefreshTokenService;
import com.delma.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final UserReposistory userReposistory;
    private final  AuthUtil authUtil;
    private final HttpServletRequest request;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequest) {
        // Implement login logic here
        LoginResponseDTO loginResponse = authService.login(loginRequest);
        log.info("User {} logged in successfully", loginRequest.getEmail());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        // IMPORTANT: Do NOT send refreshToken in body
        LoginResponseDTO safeResponse = new LoginResponseDTO(
                loginResponse.getJwtToken(),
                loginResponse.getUserId(),
                loginResponse.getRole(),
                loginResponse.getIsAdmin(),
                loginResponse.getUsername(),
                null
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(
                        ApiResponse.success(
                                safeResponse,
                                "Login successful"
                        )
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> signOut(HttpServletResponse response){
        log.info("Logout endpoint request receving");
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE,deleteCookie.toString());

        String userIdHeader = request.getHeader("X-User-Id");
        log.info("User ID: {}",userIdHeader);
        if (userIdHeader != null) {
            Long userId = Long.parseLong(userIdHeader);
            refreshTokenService.deleteAllByUser(userId);
        }

        return ResponseEntity.ok(
                ApiResponse.success("","Logged out successfully")
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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Refresh token is missing", "AUTH_401"));
        }

        log.info("Received refresh token: {}", refreshToken);

        try {
            RefreshToken storedToken = refreshTokenService.validate(refreshToken);
            log.info("Stored token found for userId: {}", storedToken.getUserId());

            User user = userReposistory
                    .findById(storedToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = authUtil.generateAccessToken(user);
            log.info("New access token: {}", newAccessToken);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            new AuthTokenResponse(newAccessToken),
                            "Token refreshed"
                    )
            );

        } catch (Exception e) {
            log.error("Error during refresh token validation: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Refresh token validation failed", "AUTH_500"));
        }
    }


}

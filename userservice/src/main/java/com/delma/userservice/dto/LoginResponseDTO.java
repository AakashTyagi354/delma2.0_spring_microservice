package com.delma.userservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    String jwtToken;
    Long userId;
    String role;
    String isAdmin;
    String username;

    @JsonIgnore
    private String refreshToken;
}

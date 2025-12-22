package com.delma.userservice.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String name;
    private String password;
}

package com.delma.userservice.dto;

import com.delma.userservice.Enum.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String username;
    private Set<Role> roles;
}

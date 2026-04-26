package com.delma.userservice.service;

import com.delma.userservice.dto.UserResponse;
import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;

import java.util.List;

public interface UserService {
     UserResponse createUser(User user);
     UserResponse getUserById(Long id);

     void applyDoctor(Doctor request, String email);
     void addRoleDoctor(Long userId);

     List<UserResponse> findAllUsers();

}

package com.delma.userservice.service;

import com.delma.userservice.dto.DoctorResponseDTO;
import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;

import java.util.List;

public interface UserService {
    public User createUser(User user);
    public User getUserById(Long id);

    public void applyDoctor(Doctor request, String email);
    public void addRoleDoctor(Long userId);

}

package com.delma.userservice.service;

import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;

public interface UserService {
    public User createUser(User user);
    public User getUserById(Long id);

    public void applyDoctor(Doctor request, String email);
    public void addRoleDoctor(Long userId);
}

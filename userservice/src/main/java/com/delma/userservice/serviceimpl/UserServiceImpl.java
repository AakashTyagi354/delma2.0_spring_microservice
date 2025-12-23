package com.delma.userservice.serviceimpl;

import com.delma.userservice.Enum.Role;
import com.delma.userservice.dto.DoctorResponseDTO;
import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import com.delma.userservice.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserReposistory userReposistory;

    public User createUser(User user) {
        return userReposistory.save(user);
    }

    public User getUserById(Long id) {
        return userReposistory.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void applyDoctor(Doctor request, String email) {

    }

    public void addRoleDoctor(Long userId) {
        User user = userReposistory.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.getRoles().add(Role.DOCTOR);  // assuming Role is your enum
        userReposistory.save(user);
    }


}

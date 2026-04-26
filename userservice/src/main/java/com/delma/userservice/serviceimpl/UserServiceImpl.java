package com.delma.userservice.serviceimpl;

import com.delma.common.exception.ResourceNotFoundException;
import com.delma.userservice.Enum.Role;
import com.delma.userservice.dto.UserResponse;
import com.delma.userservice.entity.Doctor;
import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import com.delma.userservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserReposistory userReposistory;

    @Override
    @Transactional
    public UserResponse createUser(User user) {
        User newUser = userReposistory.save(user);
        return toResponse(newUser);
    }

    public UserResponse getUserById(Long id) {
        User user =  userReposistory.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for userId: "+id));
        return toResponse(user);
    }

    @Override
    public void applyDoctor(Doctor request, String email) {

    }

    @Override
    @Transactional
    public void addRoleDoctor(Long userId) {
        User user = userReposistory.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for userId: " + userId));

        user.getRoles().add(Role.DOCTOR);  // assuming Role is your enum
        userReposistory.save(user);
    }

    @Override
    public List<UserResponse> findAllUsers() {
       List<User> users =  userReposistory.findByIsDoctorAndIsAdmin("false","false");
       return users.stream()
               .map(this::toResponse)
               .toList();
    }

    private UserResponse toResponse(User user){
        return UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles())
                .build();

    }

}

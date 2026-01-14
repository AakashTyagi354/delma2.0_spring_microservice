package com.delma.doctorservice.repository;

import com.delma.doctorservice.Enum.ApplicationStatus;
import com.delma.doctorservice.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByUserIdAndStatus(String userId, ApplicationStatus status);
    List<Doctor> findAllByStatus(ApplicationStatus status);
    List<Doctor> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(String firstName, String lastName, String specialization);
    Optional<Doctor> findByUserId(Long userId);
}

package com.delma.doctorservice.service;

import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.entity.Doctor;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface DoctorService {
    public void submitApplication(String userId, DoctorApplicationRequest request);
    public void approveApplication(Long applicationId);
    public void rejectApplication(Long applicationId);
    public List<Doctor> getPendingApplications();

    public List<Doctor> getAllDoctors();
}

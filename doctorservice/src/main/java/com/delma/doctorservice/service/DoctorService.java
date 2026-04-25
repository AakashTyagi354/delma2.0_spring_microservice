package com.delma.doctorservice.service;

import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.dto.DoctorResponse;
import com.delma.doctorservice.entity.Doctor;


import java.util.List;

public interface DoctorService {
     void submitApplication(String userId, DoctorApplicationRequest request);
     void approveApplication(Long applicationId);
     void rejectApplication(Long applicationId);
     List<DoctorResponse> getPendingApplications();

     List<DoctorResponse> getAllDoctors();
     List<DoctorResponse> getAllPendingDoctors();

     List<DoctorResponse> searchDoctors(String keyword);
}

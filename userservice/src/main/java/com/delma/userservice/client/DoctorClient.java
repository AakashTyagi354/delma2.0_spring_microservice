package com.delma.userservice.client;



import com.delma.userservice.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "doctorservice",
        url = "http://localhost:8010"
)
public interface DoctorClient {
    @PutMapping("/api/v1/doctor/approve/{doctorId}")
    void approveDoctor(@PathVariable String doctorId);

    @PostMapping("/api/v1/doctor/reject/{id}")
    String rejectApplication(@PathVariable Long id);

    @GetMapping("/api/v1/doctor/pending")
    List<DoctorResponseDTO> getPendingApplications();


    @GetMapping("/api/v1/doctor/all")
    List<DoctorResponseDTO> getAllDoctors();



}

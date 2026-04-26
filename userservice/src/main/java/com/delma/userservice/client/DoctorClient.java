package com.delma.userservice.client;



import com.delma.common.dto.ApiResponse;
import com.delma.userservice.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "doctorservice"
)
public interface DoctorClient {
    @PutMapping("/api/v1/doctor/approve/{doctorId}")
    ApiResponse<Void> approveDoctor(@PathVariable String doctorId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/doctor/reject/{id}")
    ApiResponse<Void> rejectApplication(@PathVariable Long id);

    @GetMapping("/api/v1/doctor/pending")
    ApiResponse<List<DoctorResponseDTO>> getPendingApplications();


    @GetMapping("/api/v1/doctor/all")
    ApiResponse<List<DoctorResponseDTO>> getAllDoctors();



}

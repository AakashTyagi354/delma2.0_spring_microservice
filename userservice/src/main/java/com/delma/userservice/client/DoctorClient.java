package com.delma.userservice.client;



import com.delma.userservice.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "doctor-service",
        url = "http://localhost:8002"
)
public interface DoctorClient {
    @PutMapping("/api/v1/doctor/approve/{doctorId}")
    void approveDoctor(@PathVariable String doctorId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/doctor/reject/{id}")
    String rejectApplication(@PathVariable Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/doctor/pending")
    List<DoctorResponseDTO> getPendingApplications(@RequestHeader("Authorization") String token);



}

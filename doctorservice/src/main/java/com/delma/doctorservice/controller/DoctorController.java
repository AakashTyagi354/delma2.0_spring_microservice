package com.delma.doctorservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.dto.DoctorResponse;
import com.delma.doctorservice.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void>> applyDoctor(@RequestParam String userId,
                                              @RequestBody DoctorApplicationRequest request) {

        doctorService.submitApplication(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully"));
    }

    // Admin endpoints
//    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        doctorService.approveApplication(id);
        return ResponseEntity.ok(ApiResponse.success("Application approved"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id) {
        doctorService.rejectApplication(id);
        return ResponseEntity.ok(ApiResponse.success("Application rejected"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> pendingApplications() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getPendingApplications(),"Getting pending Doctor applications"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllDoctors(),"Getting all the doctors"));
    }

    @GetMapping("/pending-doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllPendingDoctors() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllPendingDoctors(),"Doctors fetched successfully"));
    }


    @GetMapping("/search/{keyword}")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> searchDoctors(@PathVariable String keyword){
            List<DoctorResponse> doctors = doctorService.searchDoctors(keyword);

        return ResponseEntity.ok(
                ApiResponse.success(doctors, doctors.isEmpty()
                        ? "No doctors found matching: " + keyword
                        : "Doctors fetched successfully")
        );
    }
}

package com.delma.doctorservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "user-service",
        url = "http://localhost:8001"
)
public interface UserServiceClient {

    @PutMapping("/api/v1/admin/add-role/doctor/{userId}")
    void addDoctorRole(@PathVariable String userId, @RequestHeader("Authorization") String token);

}

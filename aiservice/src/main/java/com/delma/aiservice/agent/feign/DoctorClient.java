package com.delma.aiservice.agent.feign;


import com.delma.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "doctorservice")
public interface DoctorClient {

    @GetMapping("/api/v1/doctor/search/{keyword}")
    ApiResponse<List<Map<String, Object>>> searchDoctors(
            @PathVariable("keyword") String keyword);

    @GetMapping("/api/v1/doctor/all")
    ApiResponse<List<Map<String, Object>>> getAllDoctors();

}

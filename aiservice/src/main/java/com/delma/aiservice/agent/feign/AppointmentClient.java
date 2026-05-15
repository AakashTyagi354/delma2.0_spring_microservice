package com.delma.aiservice.agent.feign;


import com.delma.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@FeignClient(name =  "appointmentservice")
public interface AppointmentClient {
    @GetMapping("/api/v1/appointments/slots")
    ApiResponse<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date);

    @PostMapping("/api/v1/appointments/book")
    ApiResponse<Map<String, Object>> bookAppointment(
            @RequestParam("userId") Long userId,
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("slotId") Long slotId);

    @GetMapping("/api/v1/appointments/user")
    ApiResponse<List<Map<String, Object>>> getUserAppointments(
            @RequestParam("userId") Long userId);

}

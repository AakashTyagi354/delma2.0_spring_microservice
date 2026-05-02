package com.delma.appointmentservice.dto;


import com.delma.appointmentservice.utility.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppointmentResponse {
    private Long id;
    private Long userId;
    private Long doctorId;
    private Long slotId;
    private AppointmentStatus status;
}

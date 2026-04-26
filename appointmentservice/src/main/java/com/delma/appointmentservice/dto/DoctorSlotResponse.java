package com.delma.appointmentservice.dto;


import com.delma.appointmentservice.utility.SlotStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class DoctorSlotResponse {
    private Long doctorId;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;
}

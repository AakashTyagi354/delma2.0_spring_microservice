package com.delma.appointmentservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRecord {
    private String drugName;
    private String genericName;
    private String dose;
    private String frequency;
    private Integer durationDays;
    private String route;
    private String notes;
}

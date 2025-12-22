package com.delma.doctorservice.dto;

import lombok.Data;

@Data
public class DoctorApplicationRequest {
    private String specialization;
    private int experience;
}

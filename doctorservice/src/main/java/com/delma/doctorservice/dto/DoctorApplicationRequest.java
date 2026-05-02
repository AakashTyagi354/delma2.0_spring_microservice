package com.delma.doctorservice.dto;

import lombok.Data;

@Data
public class DoctorApplicationRequest {
    private String name;
    private String email;
    private String phoneNo;
    private String Address;
    private Double feesPerCunsaltation;
    private String specialization;
    private int experience;
}

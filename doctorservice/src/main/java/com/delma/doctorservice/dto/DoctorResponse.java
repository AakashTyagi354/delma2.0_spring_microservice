package com.delma.doctorservice.dto;


import com.delma.doctorservice.Enum.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class DoctorResponse {
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String specialization;
    private Integer experience;
    private Double feesPerConsultation;
    private String gender;
    private String address;
    private String website;
    private ApplicationStatus status;
}

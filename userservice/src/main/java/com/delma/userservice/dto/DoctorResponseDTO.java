package com.delma.userservice.dto;

import com.delma.userservice.Enum.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponseDTO {
    private Long id;

    private String userId;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private String website;

    private String address;

    private String specialization;

    private Integer experience;

    private Double feesPerConsultation;

    private String gender;

    private ApplicationStatus status;
}

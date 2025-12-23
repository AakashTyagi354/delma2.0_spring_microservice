package com.delma.appointmentservice.entity;


import com.delma.appointmentservice.utility.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long doctorId;
    private Long slotId;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private LocalDateTime createdAt;
}

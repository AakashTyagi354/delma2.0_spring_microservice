package com.delma.appointmentservice.entity;


import com.delma.appointmentservice.utility.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments",
    indexes = {
            @Index(name = "idx_appointment_user_id",columnList = "userId"),
            @Index(name = "idx_appointment_doctor_id", columnList = "doctorId"),
            @Index(
                    name = "idx_appointment_doctor_slot_status",
                    columnList = "doctorId, slotId, status"
            )
    }
)
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

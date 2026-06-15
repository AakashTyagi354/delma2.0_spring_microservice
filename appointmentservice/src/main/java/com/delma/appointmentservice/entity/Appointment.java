package com.delma.appointmentservice.entity;


import com.delma.appointmentservice.utility.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment)) return false;
        Appointment that = (Appointment) o;
        return id != null && id.equals(that.id);  // null guard here
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();  // constant — never changes
    }
}

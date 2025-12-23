package com.delma.appointmentservice.repository;


import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.utility.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot,Long> {
    Optional<DoctorSlot> findByIdAndStatus(Long id, SlotStatus status);
    List<DoctorSlot> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    List<DoctorSlot> findByDoctorIdAndDateAndStatus(Long doctorId, LocalDate date, SlotStatus status);
    Boolean existsByDoctorIdAndDateAndStartTimeAndEndTime(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime);
}

package com.delma.appointmentservice.serviceImpl;

import com.delma.appointmentservice.entity.DoctorSlot;
import com.delma.appointmentservice.repository.DoctorSlotRepository;
import com.delma.appointmentservice.service.SlotService;
import com.delma.appointmentservice.utility.SlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;


@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {
    private final DoctorSlotRepository slotRepository;


//    /Just pre-generates slots for a doctor on a given day.

    @Override
    public void createSlots(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, int slotDurationMinutes) {
        LocalTime currentTime = startTime;
        while (!currentTime.isAfter(endTime.minusMinutes(slotDurationMinutes))) {
            LocalTime slotEndTime = currentTime.plusMinutes(slotDurationMinutes);

            // Check if slot already exists
            boolean exists = slotRepository.existsByDoctorIdAndDateAndStartTimeAndEndTime(
                    doctorId, date, currentTime, slotEndTime
            );

            if (!exists) {
                DoctorSlot slot = DoctorSlot.builder()
                        .doctorId(doctorId)
                        .date(date)
                        .startTime(currentTime)
                        .endTime(slotEndTime)
                        .status(SlotStatus.AVAILABLE)
                        .build();

                slotRepository.save(slot);
            }

            currentTime = slotEndTime;
        }

    }
}

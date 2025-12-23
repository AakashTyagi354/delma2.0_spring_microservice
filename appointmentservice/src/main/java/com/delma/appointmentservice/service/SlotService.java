package com.delma.appointmentservice.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotService {
    public void createSlots(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, int slotDurationMinutes);
}

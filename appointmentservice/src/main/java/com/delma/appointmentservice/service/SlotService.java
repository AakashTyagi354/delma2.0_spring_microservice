package com.delma.appointmentservice.service;

import java.time.LocalDate;
import java.time.LocalTime;


public interface SlotService {
     void createSlots(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, int slotDurationMinutes);
}

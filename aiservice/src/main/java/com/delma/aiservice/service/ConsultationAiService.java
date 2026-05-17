package com.delma.aiservice.service;

import com.delma.aiservice.kafka.ConsultationNotesEvent;

public interface ConsultationAiService {
    void processNotes(ConsultationNotesEvent event);
}

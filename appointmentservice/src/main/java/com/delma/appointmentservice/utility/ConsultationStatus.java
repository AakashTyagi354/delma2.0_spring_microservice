package com.delma.appointmentservice.utility;


//
// Tracks the lifecycle of consultation notes:
//
// DRAFT        → doctor is typing during the call (auto-saved every 30s)
// SAVED        → doctor clicked "End Call & Save" — triggers AI processing
// AI_PROCESSING → aiservice is expanding the notes (Kafka consumed)
// AI_PROCESSED  → AI report generated and saved back
// SENT          → patient has been notified via email

public enum ConsultationStatus {
    DRAFT,
    SAVED,
    AI_PROCESSING,
    AI_PROCESSED,
    SENT
}

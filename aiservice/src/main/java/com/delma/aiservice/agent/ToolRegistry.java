package com.delma.aiservice.agent;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// ToolRegistry
//
// Defines all tools available to the AI agent.
// Each tool maps to a real API endpoint in another microservice.
//
// The description is critical — it tells the LLM WHEN to use each tool.
// Poorly written descriptions = LLM calls wrong tool or misses it entirely.
//
// Fix applied:
//   - doctorId is now always an integer in tool descriptions
//   - searchDoctors result renames userId → doctorId (integer) in ToolExecutor
//   - descriptions updated to reflect doctorId field directly
// ─────────────────────────────────────────────────────────────────────────────

@Component
public class ToolRegistry {

    public List<ToolDefinition> getTools() {
        return List.of(
                searchDoctorsTool(),
                getAvailableSlotsTool(),
                bookAppointmentTool(),
                getMyAppointmentsTool(),
                createPaymentOrderTool()
        );
    }

    private ToolDefinition searchDoctorsTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDef.builder()
                        .name("search_doctors")
                        .description("""
                            Search for doctors by medical specialization or name.
                            Use this when the user mentions a type of doctor or condition.
                            Examples: 'cardiologist', 'heart doctor', 'skin specialist', 'cardiology'
                            Returns a list of doctors. Each doctor has a 'doctorId' (integer)
                            which must be used in get_available_slots and book_appointment.
                            """)
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of(
                                                "type", "string",
                                                "description",
                                                "Specialization or doctor name to search for e.g. cardiology, dermatology"
                                        )
                                ),
                                "required", List.of("keyword")
                        ))
                        .build())
                .build();
    }

    private ToolDefinition getAvailableSlotsTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDef.builder()
                        .name("get_available_slots")
                        .description("""
                            Get available appointment slots for a specific doctor on a specific date.
                            Use this after finding a doctor with search_doctors.
                            Always call this before booking to confirm slot availability.
                            Use the 'doctorId' integer field directly from search_doctors result.
                            Do NOT use the doctor's 'id' field — use 'doctorId' only.
                            """)
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "doctorId", Map.of(
                                                "type", "integer",
                                                "description",
                                                "The doctorId integer from search_doctors result. " +
                                                        "It is already an integer — use it directly."
                                        ),
                                        "date", Map.of(
                                                "type", "string",
                                                "description",
                                                "Date in YYYY-MM-DD format e.g. 2026-05-20. " +
                                                        "Today is " + java.time.LocalDate.now()
                                        )
                                ),
                                "required", List.of("doctorId", "date")
                        ))
                        .build())
                .build();
    }

    private ToolDefinition bookAppointmentTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDef.builder()
                        .name("book_appointment")
                        .description("""
                            Book an appointment for the user with a specific doctor at a specific slot.
                            Only call this after confirming the user wants to proceed.
                            Always show the doctor name, date and time before booking and ask for confirmation.
                            Use the 'doctorId' integer field from search_doctors result — it is an integer.
                            Use the 'id' integer field from get_available_slots result as slotId.
                            """)
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "doctorId", Map.of(
                                                "type", "integer",
                                                "description",
                                                "The doctorId integer from search_doctors result. " +
                                                        "It is already an integer — use it directly."
                                        ),
                                        "slotId", Map.of(
                                                "type", "integer",
                                                "description",
                                                "The 'id' integer field from get_available_slots result. " +
                                                        "Use the exact integer value — do not convert to string."
                                        )
                                ),
                                "required", List.of("doctorId", "slotId")
                        ))
                        .build())
                .build();
    }

    private ToolDefinition getMyAppointmentsTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDef.builder()
                        .name("get_my_appointments")
                        .description("""
                            Get the user's upcoming and past appointments.
                            Use this when user asks about their bookings, schedule, or appointments.
                            """)
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(),
                                "required", List.of()
                        ))
                        .build())
                .build();
    }

    private ToolDefinition createPaymentOrderTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDef.builder()
                        .name("create_payment_order")
                        .description("""
                            Creates a Razorpay payment order for booking an appointment.
                            Call this after user confirms they want to book a specific slot.
                            Returns a payment order that the user must complete.
                            Use the 'doctorId' integer from search_doctors result.
                            Use the 'id' integer from get_available_slots result as slotId.
                            """)
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "doctorId", Map.of(
                                                "type", "integer",
                                                "description",
                                                "The doctorId integer from search_doctors result."
                                        ),
                                        "slotId", Map.of(
                                                "type", "integer",
                                                "description",
                                                "The 'id' integer from get_available_slots result."
                                        ),
                                        "amount", Map.of(
                                                "type", "integer",
                                                "description",
                                                "Amount in paise e.g. 50000 for ₹500"
                                        )
                                ),
                                "required", List.of("doctorId", "slotId", "amount")
                        ))
                        .build())
                .build();
    }
}
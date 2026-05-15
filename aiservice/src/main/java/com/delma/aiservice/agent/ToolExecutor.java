package com.delma.aiservice.agent;

import com.delma.aiservice.agent.feign.AppointmentClient;
import com.delma.aiservice.agent.feign.DoctorClient;
import com.delma.aiservice.agent.feign.PaymentClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// ToolExecutor
//
// Executes tools by calling actual microservices via Feign.
// The LLM tells us WHICH tool to call and with WHAT arguments.
// We execute it and return the result back to the LLM.
// ─────────────────────────────────────────────────────────────────────────────

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final DoctorClient doctorClient;
    private final AppointmentClient appointmentClient;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    public String execute(String toolName, String argsJson, String userId) {
        log.info("Executing tool: {} with args: {}", toolName, argsJson);
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            return switch (toolName) {
                case "search_doctors"      -> searchDoctors(args);
                case "get_available_slots" -> getAvailableSlots(args);
                case "book_appointment"    -> bookAppointment(args, userId);
                case "get_my_appointments" -> getMyAppointments(userId);
                case "create_payment_order" -> createPaymentOrder(args);
                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", toolName, e.getMessage());
            return "Tool execution failed: " + e.getMessage();
        }
    }

    private String searchDoctors(Map<String, Object> args) throws Exception {
        String keyword = (String) args.get("keyword");
        var result = doctorClient.searchDoctors(keyword);
        return objectMapper.writeValueAsString(result.getData());
    }

    private String getAvailableSlots(Map<String, Object> args) throws Exception {
        Long doctorId = Long.valueOf(args.get("doctorId").toString());
        LocalDate date = LocalDate.parse((String) args.get("date"));
        var result = appointmentClient.getAvailableSlots(doctorId, date);
        return objectMapper.writeValueAsString(result.getData());
    }

    private String bookAppointment(Map<String, Object> args, String userId)
            throws Exception {
        Long doctorId = Long.valueOf(args.get("doctorId").toString());
        Long slotId   = Long.valueOf(args.get("slotId").toString());
        Long userIdL  = Long.valueOf(userId);
        var result = appointmentClient.bookAppointment(userIdL, doctorId, slotId);
        return objectMapper.writeValueAsString(result.getData());
    }

    private String getMyAppointments(String userId) throws Exception {
        var result = appointmentClient.getUserAppointments(Long.valueOf(userId));
        return objectMapper.writeValueAsString(result.getData());
    }
    private String createPaymentOrder(Map<String, Object> args) throws Exception {
        Map<String, Object> request = Map.of(
                "amount", args.get("amount"),
                "currency", "INR",
                "type", "APPOINTMENT",
                "referenceId", args.get("slotId")
        );
        var result = paymentClient.createOrder(request);
        return objectMapper.writeValueAsString(result.getData());
    }

}
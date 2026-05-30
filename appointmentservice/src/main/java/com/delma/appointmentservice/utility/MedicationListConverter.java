package com.delma.appointmentservice.utility;


import com.delma.appointmentservice.dto.MedicationRecord;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Convert
public class MedicationListConverter implements AttributeConverter<List<MedicationRecord>,String> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<MedicationRecord> list) {
        try {
            if (list == null || list.isEmpty()) return "[]";
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }


    @Override
    public List<MedicationRecord> convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return mapper.readValue(
                    json,
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, MedicationRecord.class)
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}


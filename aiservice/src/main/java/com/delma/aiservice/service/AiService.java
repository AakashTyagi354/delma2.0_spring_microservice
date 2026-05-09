package com.delma.aiservice.service;

import com.delma.aiservice.dto.SymptomRequest;
import com.delma.aiservice.dto.SymptomResponse;

public interface AiService {
    SymptomResponse analyzeSymptoms(SymptomRequest request);
}
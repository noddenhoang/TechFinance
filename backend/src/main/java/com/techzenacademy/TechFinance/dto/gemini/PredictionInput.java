package com.techzenacademy.TechFinance.dto.gemini;

import lombok.Data;

@Data
public class PredictionInput {
    private String businessData;
    private String timePeriod;
    private String conversationId;
} 
package com.techzenacademy.TechFinance.dto.gemini;

import java.util.Map;

import lombok.Data;

/**
 * DTO đại diện cho yêu cầu gửi đến Gemini API.
 */
@Data
public class GeminiRequestDTO {
    /**
     * Nội dung tin nhắn/câu hỏi của người dùng
     */
    private String userMessage;
    
    /**
     * Loại yêu cầu (chat, dự đoán, phân tích...)
     */
    private String requestType;
    
    /**
     * ID của cuộc hội thoại, có thể null nếu là cuộc hội thoại mới
     */
    private String conversationId;
    
    /**
     * Các tham số bổ sung dành cho yêu cầu đặc biệt (dự đoán, phân tích...)
     */
    private Map<String, Object> additionalParams;
    
    private boolean includeFinancialData;
    private boolean isPrediction;

    public boolean isIncludeFinancialData() {
        return includeFinancialData;
    }

    public void setIncludeFinancialData(boolean includeFinancialData) {
        this.includeFinancialData = includeFinancialData;
    }

    public boolean isPrediction() {
        return isPrediction;
    }

    public void setPrediction(boolean isPrediction) {
        this.isPrediction = isPrediction;
    }
} 
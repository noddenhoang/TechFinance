package com.techzenacademy.TechFinance.dto.gemini;

import java.util.List;

import lombok.Data;

/**
 * DTO for Gemini API response.
 */
@Data
public class GeminiResponseDTO {
    /**
     * Success status
     */
    private boolean success;
    
    /**
     * Response content from Gemini
     */
    private String response;
    
    /**
     * Error message (if any)
     */
    private String error;
    
    /**
     * Conversation ID
     */
    private String conversationId;
    
    /**
     * List of messages in the conversation
     */
    private List<GeminiMessageDTO> messages;
} 
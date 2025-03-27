package com.techzenacademy.TechFinance.dto.gemini;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GeminiMessageDTO {
    private Long id;
    private String content;
    private boolean isUser;
    private LocalDateTime timestamp;
} 
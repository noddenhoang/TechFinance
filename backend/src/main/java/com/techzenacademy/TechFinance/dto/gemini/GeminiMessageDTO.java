package com.techzenacademy.TechFinance.dto.gemini;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GeminiMessageDTO {
    private Long id;
    private String content;
    private boolean isUser;  // This is the property name used in JSON
    private LocalDateTime timestamp;
    
    // Make sure we have both getter methods with proper naming
    public boolean isUser() {
        return this.isUser;
    }
    
    public boolean getIsUser() {
        return this.isUser;
    }
    
    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
    }
}
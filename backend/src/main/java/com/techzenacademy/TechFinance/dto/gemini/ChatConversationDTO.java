package com.techzenacademy.TechFinance.dto.gemini;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * DTO đại diện cho thông tin cuộc hội thoại chat với Gemini AI
 */
@Data
public class ChatConversationDTO {
    
    private String id;
    private String firstMessage;
    private String title;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int messageCount;
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFirstMessage() {
        return firstMessage;
    }
    
    public void setFirstMessage(String firstMessage) {
        this.firstMessage = firstMessage;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
} 
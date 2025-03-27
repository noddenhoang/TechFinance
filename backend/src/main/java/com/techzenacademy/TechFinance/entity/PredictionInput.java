package com.techzenacademy.TechFinance.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entity đại diện cho dữ liệu đầu vào cho dự đoán doanh thu
 */
@Entity
@Table(name = "prediction_inputs")
@Data
public class PredictionInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "input_id")
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;
    
    @Column(name = "business_data", columnDefinition = "TEXT")
    private String businessData;
    
    @Column(name = "time_period")
    private String timePeriod;
    
    @Column(name = "industry_type")
    private String industryType;
    
    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 
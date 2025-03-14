package com.techzenacademy.TechFinance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Entity
@Table(name = "income_transactions")
@Data
public class IncomeTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private IncomeCategory category;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    private String description;
    
    @Column(name = "reference_no")
    private String referenceNo;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentStatus {
        RECEIVED("RECEIVED"),
        PENDING("PENDING");
        
        private final String value;
        
        PaymentStatus(String value) {
            this.value = value;
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        @JsonCreator
        public static PaymentStatus fromValue(String value) {
            for (PaymentStatus status : values()) {
                if (status.name().equals(value) || status.getValue().equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown PaymentStatus: " + value);
        }
    }
}
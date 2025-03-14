package com.techzenacademy.TechFinance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_reports")
@Data
public class FinancialReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;
    
    @Column(nullable = false)
    private Integer year;
    
    private Integer month;
    
    @Column(name = "total_income")
    private BigDecimal totalIncome = BigDecimal.ZERO;
    
    @Column(name = "total_expense")
    private BigDecimal totalExpense = BigDecimal.ZERO;
    
    @Column(name = "profit_loss")
    private BigDecimal profitLoss = BigDecimal.ZERO;
    
    private BigDecimal receivables = BigDecimal.ZERO;
    
    private BigDecimal payables = BigDecimal.ZERO;
    
    @Column(name = "report_data", columnDefinition = "JSON")
    private String reportData;
    
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
    
    public enum ReportType {
        INCOME,
        EXPENSE,
        SUMMARY,
        TAX
    }
}
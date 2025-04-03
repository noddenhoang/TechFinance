package com.techzenacademy.TechFinance.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransactionSummaryDTO {
    private Integer year;
    private Integer month;
    private Integer transactionCount;
    private BigDecimal amount;
    private BigDecimal percentage;
}
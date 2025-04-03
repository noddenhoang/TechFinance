package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionErrorDTO {
    private Integer transactionId;
    private String errorType; // DUPLICATE, MISSING_INFO, UNUSUAL_AMOUNT, etc.
    private String description;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String transactionType; // INCOME or EXPENSE
    private String referenceNo;
}
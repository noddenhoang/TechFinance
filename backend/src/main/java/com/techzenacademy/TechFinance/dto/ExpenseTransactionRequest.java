package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.ExpenseTransaction.PaymentStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseTransactionRequest {
    @NotNull(message = "Category ID is required")
    private Integer categoryId;
    
    private Integer supplierId;
    
    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private String description;
    private String referenceNo;
}
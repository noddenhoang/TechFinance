package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.IncomeTransaction.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeTransactionDTO {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private Integer customerId;
    private String customerName;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String description;
    private String referenceNo;
}
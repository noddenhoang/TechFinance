package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.ExpenseTransaction.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseTransactionDTO {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private Integer supplierId;
    private String supplierName;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String description;
    private String referenceNo;
}
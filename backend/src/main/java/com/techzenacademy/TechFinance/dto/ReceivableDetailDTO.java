package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReceivableDetailDTO {
    private Integer transactionId;
    private String customerName;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private Long daysOutstanding;
    private String referenceNo;
}
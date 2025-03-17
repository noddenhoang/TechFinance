package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayableDetailDTO {
    private Integer transactionId;
    private String supplierName;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private Long daysOutstanding;
    private String referenceNo;
}
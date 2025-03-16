package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionSummaryDTO {
    private Integer year;
    private Integer month;
    private BigDecimal amount;
    private BigDecimal percentage; // % so với tổng
}
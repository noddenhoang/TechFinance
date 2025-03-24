package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DetailedTransactionSummaryDTO extends TransactionSummaryDTO {
    private BigDecimal percentage;
} 
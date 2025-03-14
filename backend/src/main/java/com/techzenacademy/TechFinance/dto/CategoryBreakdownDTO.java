package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryBreakdownDTO {
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal percentage;
}
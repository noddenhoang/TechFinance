package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategorySummaryDTO {
    private Integer categoryId;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal percentage; // % so với tổng
}
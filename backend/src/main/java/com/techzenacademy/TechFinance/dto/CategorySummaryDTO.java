package com.techzenacademy.TechFinance.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CategorySummaryDTO {
    private Integer categoryId;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal percentage; // % so với tổng
}
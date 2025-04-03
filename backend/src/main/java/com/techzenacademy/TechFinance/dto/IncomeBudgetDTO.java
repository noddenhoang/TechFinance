package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IncomeBudgetDTO {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private Integer year;
    private Integer month;
    private BigDecimal amount;
    private String notes;
}
package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryComparisonDTO {
    private String categoryName;
    private BigDecimal currentPeriodAmount;
    private BigDecimal previousPeriodAmount;
    private BigDecimal difference;
    private BigDecimal percentageChange;
    private BigDecimal currentPeriodPercentage;
    private BigDecimal previousPeriodPercentage;
}

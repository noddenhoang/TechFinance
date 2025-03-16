package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PeriodComparisonReportDTO {
    private Integer currentYear;
    private Integer currentMonth;
    private Integer previousYear;
    private Integer previousMonth;
    private BigDecimal currentTotalIncome;
    private BigDecimal previousTotalIncome;
    private BigDecimal incomeDifference;
    private BigDecimal incomePercentageChange;
    private BigDecimal currentTotalExpense;
    private BigDecimal previousTotalExpense;
    private BigDecimal expenseDifference;
    private BigDecimal expensePercentageChange;
    private List<CategoryComparisonDTO> incomeCategories;
    private List<CategoryComparisonDTO> expenseCategories;
}

package com.techzenacademy.TechFinance.dto.report;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyReportDTO {
    private Integer year;
    private Integer month;
    private ReportSummaryDTO summary;
    private List<CategoryComparisonDTO> incomeCategories;
    private List<CategoryComparisonDTO> expenseCategories;
    
    @Data
    public static class ReportSummaryDTO {
        private BigDecimal totalIncomeBudget;
        private BigDecimal totalIncomeActual;
        private BigDecimal totalExpenseBudget;
        private BigDecimal totalExpenseActual;
    }
    
    @Data
    public static class CategoryComparisonDTO {
        private Integer categoryId;
        private String categoryName;
        private BigDecimal budgetAmount;
        private BigDecimal actualAmount;
        private BigDecimal difference;
        private Double percentageOfTotal;
    }
}
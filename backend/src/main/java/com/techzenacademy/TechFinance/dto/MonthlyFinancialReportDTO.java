package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class MonthlyFinancialReportDTO {
    private Integer year;
    private Integer month;
    private BigDecimal totalIncome;
    private BigDecimal receivedIncome;
    private BigDecimal pendingIncome;
    private BigDecimal totalExpense;
    private BigDecimal paidExpense;
    private BigDecimal pendingExpense;
    private BigDecimal profitLoss;
    private BigDecimal incomeBudget;
    private BigDecimal expenseBudget;
    private BigDecimal budgetVariance;
    private Map<String, Object> categoryBreakdown;
}
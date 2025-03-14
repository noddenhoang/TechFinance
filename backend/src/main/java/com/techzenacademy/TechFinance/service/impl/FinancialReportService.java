package com.techzenacademy.TechFinance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techzenacademy.TechFinance.dto.CategoryBreakdownDTO;
import com.techzenacademy.TechFinance.dto.CategoryReportDTO;
import com.techzenacademy.TechFinance.dto.MonthlyFinancialReportDTO;
import com.techzenacademy.TechFinance.entity.FinancialReport;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.FinancialReportRepository;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialReportService {
    
    @Autowired
    private FinancialReportRepository reportRepository;
    
    @Autowired
    private IncomeTransactionRepository incomeRepository;
    
    @Autowired
    private ExpenseTransactionRepository expenseRepository;
    
    @Autowired
    private IncomeBudgetRepository incomeBudgetRepository;
    
    @Autowired
    private ExpenseBudgetRepository expenseBudgetRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public MonthlyFinancialReportDTO getMonthlyReport(Integer year, Integer month) {
        MonthlyFinancialReportDTO report = new MonthlyFinancialReportDTO();
        report.setYear(year);
        report.setMonth(month);
        
        // Validate year and month
        if (year < 2000 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid year or month");
        }
        
        // Calculate start and end dates of the month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        
        // Get income transactions for the month
        List<IncomeTransaction> incomeTransactions = 
                incomeRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        // Get expense transactions for the month
        List<ExpenseTransaction> expenseTransactions = 
                expenseRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        // Calculate income statistics
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal receivedIncome = BigDecimal.ZERO;
        BigDecimal pendingIncome = BigDecimal.ZERO;
        
        for (IncomeTransaction transaction : incomeTransactions) {
            totalIncome = totalIncome.add(transaction.getAmount());
            if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                receivedIncome = receivedIncome.add(transaction.getAmount());
            } else {
                pendingIncome = pendingIncome.add(transaction.getAmount());
            }
        }
        
        // Calculate expense statistics
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal paidExpense = BigDecimal.ZERO;
        BigDecimal pendingExpense = BigDecimal.ZERO;
        
        for (ExpenseTransaction transaction : expenseTransactions) {
            totalExpense = totalExpense.add(transaction.getAmount());
            if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                paidExpense = paidExpense.add(transaction.getAmount());
            } else {
                pendingExpense = pendingExpense.add(transaction.getAmount());
            }
        }
        
        // Calculate profit/loss
        BigDecimal profitLoss = totalIncome.subtract(totalExpense);
        
        // Get budget information
        BigDecimal incomeBudget = getIncomeBudgetTotal(year, month);
        BigDecimal expenseBudget = getExpenseBudgetTotal(year, month);
        BigDecimal budgetVariance = (totalIncome.subtract(incomeBudget))
                .subtract(totalExpense.subtract(expenseBudget));
        
        // Set calculated values to report
        report.setTotalIncome(totalIncome);
        report.setReceivedIncome(receivedIncome);
        report.setPendingIncome(pendingIncome);
        report.setTotalExpense(totalExpense);
        report.setPaidExpense(paidExpense);
        report.setPendingExpense(pendingExpense);
        report.setProfitLoss(profitLoss);
        report.setIncomeBudget(incomeBudget);
        report.setExpenseBudget(expenseBudget);
        report.setBudgetVariance(budgetVariance);
        
        // Get category breakdown
        CategoryReportDTO categoryReport = getCategoryBreakdown(year, month);
        Map<String, Object> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put("income", categoryReport.getIncomeCategories());
        categoryBreakdown.put("expense", categoryReport.getExpenseCategories());
        report.setCategoryBreakdown(categoryBreakdown);
        
        return report;
    }
    
    public List<MonthlyFinancialReportDTO> getYearlyReports(Integer year) {
        // Validate year
        if (year < 2000) {
            throw new IllegalArgumentException("Invalid year");
        }
        
        List<MonthlyFinancialReportDTO> yearlyReports = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            try {
                MonthlyFinancialReportDTO monthReport = getMonthlyReport(year, month);
                yearlyReports.add(monthReport);
            } catch (Exception e) {
                // If there's no data for a month, add an empty report
                MonthlyFinancialReportDTO emptyReport = new MonthlyFinancialReportDTO();
                emptyReport.setYear(year);
                emptyReport.setMonth(month);
                emptyReport.setTotalIncome(BigDecimal.ZERO);
                emptyReport.setTotalExpense(BigDecimal.ZERO);
                emptyReport.setProfitLoss(BigDecimal.ZERO);
                yearlyReports.add(emptyReport);
            }
        }
        
        return yearlyReports;
    }
    
    public CategoryReportDTO getCategoryBreakdown(Integer year, Integer month) {
        CategoryReportDTO report = new CategoryReportDTO();
        
        // Get income by category
        List<Object[]> incomeByCategory = reportRepository.getIncomeByCategory(year, month);
        List<CategoryBreakdownDTO> incomeCategories = new ArrayList<>();
        
        for (Object[] row : incomeByCategory) {
            CategoryBreakdownDTO category = new CategoryBreakdownDTO();
            category.setCategoryName((String) row[1]);  // category_name
            category.setAmount(new BigDecimal(row[2].toString())); // total_amount
            category.setPercentage(new BigDecimal(row[3].toString())); // percentage
            incomeCategories.add(category);
        }
        
        report.setIncomeCategories(incomeCategories);
        
        // Get expense by category
        List<Object[]> expenseByCategory = reportRepository.getExpenseByCategory(year, month);
        List<CategoryBreakdownDTO> expenseCategories = new ArrayList<>();
        
        for (Object[] row : expenseByCategory) {
            CategoryBreakdownDTO category = new CategoryBreakdownDTO();
            category.setCategoryName((String) row[1]);  // category_name
            category.setAmount(new BigDecimal(row[2].toString())); // total_amount
            category.setPercentage(new BigDecimal(row[3].toString())); // percentage
            expenseCategories.add(category);
        }
        
        report.setExpenseCategories(expenseCategories);
        
        return report;
    }
    
    private BigDecimal getIncomeBudgetTotal(Integer year, Integer month) {
        try {
            return incomeBudgetRepository.sumAmountByYearAndMonth(year, month)
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal getExpenseBudgetTotal(Integer year, Integer month) {
        try {
            return expenseBudgetRepository.sumAmountByYearAndMonth(year, month)
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
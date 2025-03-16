package com.techzenacademy.TechFinance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techzenacademy.TechFinance.dto.CategoryBreakdownDTO;
import com.techzenacademy.TechFinance.dto.CategoryComparisonDTO;
import com.techzenacademy.TechFinance.dto.CategoryReportDTO;
import com.techzenacademy.TechFinance.dto.MonthlyFinancialReportDTO;
import com.techzenacademy.TechFinance.dto.PeriodComparisonReportDTO;
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
import java.util.Comparator;
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
    
    public PeriodComparisonReportDTO comparePeriods(Integer currentYear, Integer currentMonth, 
                                              Integer previousYear, Integer previousMonth) {
        // Validate inputs
        if (currentYear < 2000 || currentMonth < 1 || currentMonth > 12 ||
            previousYear < 2000 || previousMonth < 1 || previousMonth > 12) {
            throw new IllegalArgumentException("Invalid year or month");
        }
        
        PeriodComparisonReportDTO report = new PeriodComparisonReportDTO();
        report.setCurrentYear(currentYear);
        report.setCurrentMonth(currentMonth);
        report.setPreviousYear(previousYear);
        report.setPreviousMonth(previousMonth);
        
        // Get category breakdowns for both periods
        CategoryReportDTO currentPeriod = getCategoryBreakdown(currentYear, currentMonth);
        CategoryReportDTO previousPeriod = getCategoryBreakdown(previousYear, previousMonth);
        
        // Calculate totals from the current period
        BigDecimal currentTotalIncome = calculateTotalFromCategories(currentPeriod.getIncomeCategories());
        BigDecimal currentTotalExpense = calculateTotalFromCategories(currentPeriod.getExpenseCategories());
        
        // Calculate totals from the previous period
        BigDecimal previousTotalIncome = calculateTotalFromCategories(previousPeriod.getIncomeCategories());
        BigDecimal previousTotalExpense = calculateTotalFromCategories(previousPeriod.getExpenseCategories());
        
        // Set total values
        report.setCurrentTotalIncome(currentTotalIncome);
        report.setPreviousTotalIncome(previousTotalIncome);
        report.setCurrentTotalExpense(currentTotalExpense);
        report.setPreviousTotalExpense(previousTotalExpense);
        
        // Calculate differences and percentage changes
        report.setIncomeDifference(currentTotalIncome.subtract(previousTotalIncome));
        report.setExpenseDifference(currentTotalExpense.subtract(previousTotalExpense));
        
        if (previousTotalIncome.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal incomeChange = report.getIncomeDifference()
                .divide(previousTotalIncome, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            report.setIncomePercentageChange(incomeChange);
        } else if (currentTotalIncome.compareTo(BigDecimal.ZERO) > 0) {
            report.setIncomePercentageChange(new BigDecimal("100")); // 100% increase from zero
        } else {
            report.setIncomePercentageChange(BigDecimal.ZERO);
        }
        
        if (previousTotalExpense.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal expenseChange = report.getExpenseDifference()
                .divide(previousTotalExpense, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            report.setExpensePercentageChange(expenseChange);
        } else if (currentTotalExpense.compareTo(BigDecimal.ZERO) > 0) {
            report.setExpensePercentageChange(new BigDecimal("100")); // 100% increase from zero
        } else {
            report.setExpensePercentageChange(BigDecimal.ZERO);
        }
        
        // Compare income categories
        List<CategoryComparisonDTO> incomeComparisons = 
            compareCategories(currentPeriod.getIncomeCategories(), previousPeriod.getIncomeCategories(),
                            currentTotalIncome, previousTotalIncome);
        report.setIncomeCategories(incomeComparisons);
        
        // Compare expense categories
        List<CategoryComparisonDTO> expenseComparisons = 
            compareCategories(currentPeriod.getExpenseCategories(), previousPeriod.getExpenseCategories(),
                            currentTotalExpense, previousTotalExpense);
        report.setExpenseCategories(expenseComparisons);
        
        return report;
    }
    
    private BigDecimal calculateTotalFromCategories(List<CategoryBreakdownDTO> categories) {
        BigDecimal total = BigDecimal.ZERO;
        for (CategoryBreakdownDTO category : categories) {
            total = total.add(category.getAmount());
        }
        return total;
    }
    
    private List<CategoryComparisonDTO> compareCategories(List<CategoryBreakdownDTO> currentCategories, 
                                                         List<CategoryBreakdownDTO> previousCategories,
                                                         BigDecimal currentTotal,
                                                         BigDecimal previousTotal) {
        Map<String, CategoryBreakdownDTO> previousCategoryMap = new HashMap<>();
        for (CategoryBreakdownDTO category : previousCategories) {
            previousCategoryMap.put(category.getCategoryName(), category);
        }
        
        List<CategoryComparisonDTO> comparisons = new ArrayList<>();
        
        // Process all current categories
        for (CategoryBreakdownDTO currentCategory : currentCategories) {
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryName(currentCategory.getCategoryName());
            comparison.setCurrentPeriodAmount(currentCategory.getAmount());
            comparison.setCurrentPeriodPercentage(currentCategory.getPercentage());
            
            // Find matching category in previous period
            CategoryBreakdownDTO previousCategory = previousCategoryMap.get(currentCategory.getCategoryName());
            if (previousCategory != null) {
                comparison.setPreviousPeriodAmount(previousCategory.getAmount());
                comparison.setPreviousPeriodPercentage(previousCategory.getPercentage());
                comparison.setDifference(currentCategory.getAmount().subtract(previousCategory.getAmount()));
                
                if (previousCategory.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal percentageChange = comparison.getDifference()
                        .divide(previousCategory.getAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    comparison.setPercentageChange(percentageChange);
                } else if (currentCategory.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    comparison.setPercentageChange(new BigDecimal("100")); // 100% increase from zero
                } else {
                    comparison.setPercentageChange(BigDecimal.ZERO);
                }
                
                // Remove from map to track processed categories
                previousCategoryMap.remove(currentCategory.getCategoryName());
            } else {
                // Category did not exist in previous period
                comparison.setPreviousPeriodAmount(BigDecimal.ZERO);
                comparison.setPreviousPeriodPercentage(BigDecimal.ZERO);
                comparison.setDifference(currentCategory.getAmount());
                // New category means 100% increase if amount > 0
                if (currentCategory.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    comparison.setPercentageChange(new BigDecimal("100"));
                } else {
                    comparison.setPercentageChange(BigDecimal.ZERO);
                }
            }
            
            comparisons.add(comparison);
        }
        
        // Add remaining previous categories (those that don't exist in current period)
        for (Map.Entry<String, CategoryBreakdownDTO> entry : previousCategoryMap.entrySet()) {
            CategoryBreakdownDTO previousCategory = entry.getValue();
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryName(previousCategory.getCategoryName());
            comparison.setCurrentPeriodAmount(BigDecimal.ZERO);
            comparison.setCurrentPeriodPercentage(BigDecimal.ZERO);
            comparison.setPreviousPeriodAmount(previousCategory.getAmount());
            comparison.setPreviousPeriodPercentage(previousCategory.getPercentage());
            comparison.setDifference(BigDecimal.ZERO.subtract(previousCategory.getAmount()));
            comparison.setPercentageChange(new BigDecimal("-100")); // 100% decrease (category removed)
            
            comparisons.add(comparison);
        }
        
        // Sort by current period amount descending
        comparisons.sort(Comparator.comparing(CategoryComparisonDTO::getCurrentPeriodAmount).reversed());
        
        return comparisons;
    }
    
    public CategoryReportDTO getCategoryBreakdownWithFilters(Integer year, Integer month, 
                                                          boolean activeOnly, String sortBy) {
        CategoryReportDTO report = getCategoryBreakdown(year, month);
        List<CategoryBreakdownDTO> incomeCategories = report.getIncomeCategories();
        List<CategoryBreakdownDTO> expenseCategories = report.getExpenseCategories();
        
        // Filter out inactive categories if required
        if (activeOnly) {
            incomeCategories = incomeCategories.stream()
                .filter(cat -> cat.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
            
            expenseCategories = expenseCategories.stream()
                .filter(cat -> cat.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        }
        
        // Sort categories based on requested sort field
        Comparator<CategoryBreakdownDTO> comparator;
        if (sortBy.equalsIgnoreCase("percentage")) {
            comparator = Comparator.comparing(CategoryBreakdownDTO::getPercentage).reversed();
        } else if (sortBy.equalsIgnoreCase("name")) {
            comparator = Comparator.comparing(CategoryBreakdownDTO::getCategoryName);
        } else { // Default to amount
            comparator = Comparator.comparing(CategoryBreakdownDTO::getAmount).reversed();
        }
        
        List<CategoryBreakdownDTO> sortedIncomeCategories = new ArrayList<>(incomeCategories);
        sortedIncomeCategories.sort(comparator);
        
        List<CategoryBreakdownDTO> sortedExpenseCategories = new ArrayList<>(expenseCategories);
        sortedExpenseCategories.sort(comparator);
        
        CategoryReportDTO filteredReport = new CategoryReportDTO();
        filteredReport.setIncomeCategories(sortedIncomeCategories);
        filteredReport.setExpenseCategories(sortedExpenseCategories);
        
        return filteredReport;
    }
}
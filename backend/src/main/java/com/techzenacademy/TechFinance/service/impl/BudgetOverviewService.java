package com.techzenacademy.TechFinance.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.IncomeBudget;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;

@Service
public class BudgetOverviewService {

    @Autowired
    private IncomeBudgetRepository incomeBudgetRepository;

    @Autowired
    private ExpenseBudgetRepository expenseBudgetRepository;

    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;

    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;

    /**
     * Lấy tổng quan ngân sách
     * @param year Năm cần lấy dữ liệu
     * @param month Tháng cần lấy dữ liệu (nếu null thì lấy cả năm)
     * @return Map chứa dữ liệu tổng quan ngân sách
     */
    public Map<String, Object> getBudgetOverview(Integer year, Integer month) {
        Map<String, Object> result = new HashMap<>();
        
        // Dữ liệu thu nhập
        Map<String, Object> incomeData = getIncomeBudgetOverview(year, month);
        result.put("income", incomeData);
        
        // Dữ liệu chi tiêu
        Map<String, Object> expenseData = getExpenseBudgetOverview(year, month);
        result.put("expense", expenseData);
        
        return result;
    }
    
    /**
     * Lấy tổng quan ngân sách thu nhập
     */
    private Map<String, Object> getIncomeBudgetOverview(Integer year, Integer month) {
        Map<String, Object> incomeData = new HashMap<>();
        
        // Tổng ngân sách thu nhập
        BigDecimal totalBudget = calculateTotalIncomeBudget(year, month);
        
        // Tổng thu nhập thực tế
        BigDecimal totalActual = calculateTotalIncomeActual(year, month);
        
        // Chênh lệch
        BigDecimal difference = totalActual.subtract(totalBudget);
        
        // Tỷ lệ hoàn thành
        double percentage = 0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            try {
                percentage = totalActual.divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            } catch (Exception e) {
                percentage = 0;
            }
        }
        
        // Dữ liệu theo tháng
        List<Map<String, Object>> monthlyData = getMonthlyIncomeData(year);
        
        incomeData.put("totalBudget", totalBudget);
        incomeData.put("totalActual", totalActual);
        incomeData.put("difference", difference);
        incomeData.put("percentage", percentage);
        incomeData.put("monthlyData", monthlyData);
        
        return incomeData;
    }
    
    /**
     * Lấy tổng quan ngân sách chi tiêu
     */
    private Map<String, Object> getExpenseBudgetOverview(Integer year, Integer month) {
        Map<String, Object> expenseData = new HashMap<>();
        
        // Tổng ngân sách chi tiêu
        BigDecimal totalBudget = calculateTotalExpenseBudget(year, month);
        
        // Tổng chi tiêu thực tế
        BigDecimal totalActual = calculateTotalExpenseActual(year, month);
        
        // Chênh lệch
        BigDecimal difference = totalBudget.subtract(totalActual);
        
        // Tỷ lệ hoàn thành
        double percentage = 0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            try {
                percentage = totalActual.divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            } catch (Exception e) {
                percentage = 0;
            }
        }
        
        // Dữ liệu theo tháng
        List<Map<String, Object>> monthlyData = getMonthlyExpenseData(year);
        
        expenseData.put("totalBudget", totalBudget);
        expenseData.put("totalActual", totalActual);
        expenseData.put("difference", difference);
        expenseData.put("percentage", percentage);
        expenseData.put("monthlyData", monthlyData);
        
        return expenseData;
    }
    
    /**
     * Tính tổng ngân sách thu nhập
     */
    private BigDecimal calculateTotalIncomeBudget(Integer year, Integer month) {
        List<IncomeBudget> budgets;
        if (month != null) {
            // Tổng ngân sách cho một tháng cụ thể
            budgets = incomeBudgetRepository.findByYearAndMonth(year, month);
        } else {
            // Tổng ngân sách cho cả năm
            budgets = incomeBudgetRepository.findByYear(year);
        }
        
        return budgets.stream()
            .map(IncomeBudget::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính tổng thu nhập thực tế
     */
    private BigDecimal calculateTotalIncomeActual(Integer year, Integer month) {
        LocalDate startDate;
        LocalDate endDate;
        
        if (month != null) {
            // Chỉ tính cho một tháng cụ thể
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        } else {
            // Tính cho cả năm
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }
        
        List<IncomeTransaction> transactions = incomeTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        return transactions.stream()
            .map(IncomeTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính tổng ngân sách chi tiêu
     */
    private BigDecimal calculateTotalExpenseBudget(Integer year, Integer month) {
        List<ExpenseBudget> budgets;
        if (month != null) {
            // Tổng ngân sách cho một tháng cụ thể
            budgets = expenseBudgetRepository.findByYearAndMonth(year, month);
        } else {
            // Tổng ngân sách cho cả năm
            budgets = expenseBudgetRepository.findByYear(year);
        }
        
        return budgets.stream()
            .map(ExpenseBudget::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính tổng chi tiêu thực tế
     */
    private BigDecimal calculateTotalExpenseActual(Integer year, Integer month) {
        LocalDate startDate;
        LocalDate endDate;
        
        if (month != null) {
            // Chỉ tính cho một tháng cụ thể
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        } else {
            // Tính cho cả năm
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }
        
        List<ExpenseTransaction> transactions = expenseTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        return transactions.stream()
            .map(ExpenseTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Lấy dữ liệu thu nhập theo tháng cho biểu đồ
     */
    private List<Map<String, Object>> getMonthlyIncomeData(Integer year) {
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthData = new HashMap<>();
            
            // Tính ngân sách và thu nhập thực tế cho tháng này
            BigDecimal budget = calculateTotalIncomeBudget(year, month);
            BigDecimal actual = calculateTotalIncomeActual(year, month);
            BigDecimal difference = actual.subtract(budget);
            double percentage = 0;
            if (budget.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    percentage = actual.divide(budget, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                } catch (Exception e) {
                    percentage = 0;
                }
            }
            
            monthData.put("month", month);
            monthData.put("budget", budget);
            monthData.put("actual", actual);
            monthData.put("difference", difference);
            monthData.put("percentage", percentage);
            
            monthlyData.add(monthData);
        }
        
        return monthlyData;
    }
    
    /**
     * Lấy dữ liệu chi tiêu theo tháng cho biểu đồ
     */
    private List<Map<String, Object>> getMonthlyExpenseData(Integer year) {
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthData = new HashMap<>();
            
            // Tính ngân sách và chi tiêu thực tế cho tháng này
            BigDecimal budget = calculateTotalExpenseBudget(year, month);
            BigDecimal actual = calculateTotalExpenseActual(year, month);
            BigDecimal difference = budget.subtract(actual);
            double percentage = 0;
            if (budget.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    percentage = actual.divide(budget, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                } catch (Exception e) {
                    percentage = 0;
                }
            }
            
            monthData.put("month", month);
            monthData.put("budget", budget);
            monthData.put("actual", actual);
            monthData.put("difference", difference);
            monthData.put("percentage", percentage);
            
            monthlyData.add(monthData);
        }
        
        return monthlyData;
    }
} 
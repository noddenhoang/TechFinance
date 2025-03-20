package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO;
import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO.*;
import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.IncomeBudget;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialReportService {
    
    @Autowired
    private IncomeBudgetRepository incomeBudgetRepository;
    
    @Autowired
    private ExpenseBudgetRepository expenseBudgetRepository;
    
    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;
    
    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;
    
    /**
     * Tạo báo cáo tài chính tháng
     */
    public MonthlyReportDTO generateMonthlyReport(Integer year, Integer month) {
        // Validation
        if (year < 2000 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid year or month");
        }
        
        MonthlyReportDTO report = new MonthlyReportDTO();
        report.setYear(year);
        report.setMonth(month);
        
        // Tính toán khoảng thời gian
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        
        // 1. Tính tổng thu nhập theo ngân sách
        List<IncomeBudget> incomeBudgets = incomeBudgetRepository.findByYearAndMonth(year, month);
        BigDecimal totalIncomeBudget = incomeBudgets.stream()
                .map(IncomeBudget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 2. Tính tổng chi tiêu theo ngân sách
        List<ExpenseBudget> expenseBudgets = expenseBudgetRepository.findByYearAndMonth(year, month);
        BigDecimal totalExpenseBudget = expenseBudgets.stream()
                .map(ExpenseBudget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 3. Tính thu nhập thực tế (chỉ tính đã thanh toán)
        List<IncomeTransaction> incomeTransactions = incomeTransactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        BigDecimal totalIncomeActual = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 4. Tính chi tiêu thực tế (chỉ tính đã thanh toán)
        List<ExpenseTransaction> expenseTransactions = expenseTransactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        BigDecimal totalExpenseActual = expenseTransactions.stream()
                .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID)
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 5. Tạo summary
        ReportSummaryDTO summary = new ReportSummaryDTO();
        summary.setTotalIncomeBudget(totalIncomeBudget);
        summary.setTotalIncomeActual(totalIncomeActual);
        summary.setTotalExpenseBudget(totalExpenseBudget);
        summary.setTotalExpenseActual(totalExpenseActual);
        report.setSummary(summary);
        
        // 6. Chi tiết theo danh mục thu nhập
        report.setIncomeCategories(generateIncomeCategoryComparisons(incomeBudgets, incomeTransactions, totalIncomeActual));
        
        // 7. Chi tiết theo danh mục chi tiêu
        report.setExpenseCategories(generateExpenseCategoryComparisons(expenseBudgets, expenseTransactions, totalExpenseActual));
        
        return report;
    }
    
    /**
     * Tạo danh sách so sánh theo danh mục thu nhập
     */
    private List<CategoryComparisonDTO> generateIncomeCategoryComparisons(
            List<IncomeBudget> budgets, 
            List<IncomeTransaction> transactions,
            BigDecimal totalActual) {
        
        // Tạo map để tính tổng thực tế theo danh mục
        Map<Integer, BigDecimal> actualByCategory = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();
        
        for (IncomeTransaction transaction : transactions) {
            if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                Integer categoryId = transaction.getCategory().getId();
                categoryNames.put(categoryId, transaction.getCategory().getName());
                
                BigDecimal currentAmount = actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
                actualByCategory.put(categoryId, currentAmount.add(transaction.getAmount()));
            }
        }
        
        // Tạo danh sách so sánh
        List<CategoryComparisonDTO> comparisons = new ArrayList<>();
        
        // Thêm các danh mục có trong ngân sách
        for (IncomeBudget budget : budgets) {
            Integer categoryId = budget.getCategory().getId();
            
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(budget.getCategory().getName());
            comparison.setBudgetAmount(budget.getAmount());
            comparison.setActualAmount(actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO));
            comparison.setDifference(comparison.getActualAmount().subtract(comparison.getBudgetAmount()));
            
            // Tính % của tổng thực tế
            if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = comparison.getActualAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalActual, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                comparison.setPercentageOfTotal(percentage);
            } else {
                comparison.setPercentageOfTotal(0.0);
            }
            
            comparisons.add(comparison);
            
            // Xóa khỏi map để theo dõi những gì đã được xử lý
            actualByCategory.remove(categoryId);
        }
        
        // Thêm các danh mục có giao dịch thực tế nhưng không có trong ngân sách
        for (Map.Entry<Integer, BigDecimal> entry : actualByCategory.entrySet()) {
            Integer categoryId = entry.getKey();
            BigDecimal amount = entry.getValue();
            
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(categoryNames.get(categoryId));
            comparison.setBudgetAmount(BigDecimal.ZERO);
            comparison.setActualAmount(amount);
            comparison.setDifference(amount);
            
            // Tính % của tổng thực tế
            if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = amount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalActual, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                comparison.setPercentageOfTotal(percentage);
            } else {
                comparison.setPercentageOfTotal(0.0);
            }
            
            comparisons.add(comparison);
        }
        
        // Sắp xếp theo số tiền thực tế giảm dần
        comparisons.sort((a, b) -> b.getActualAmount().compareTo(a.getActualAmount()));
        
        return comparisons;
    }
    
    /**
     * Tạo danh sách so sánh theo danh mục chi tiêu
     */
    private List<CategoryComparisonDTO> generateExpenseCategoryComparisons(
            List<ExpenseBudget> budgets, 
            List<ExpenseTransaction> transactions,
            BigDecimal totalActual) {
        
        // Tạo map để tính tổng thực tế theo danh mục
        Map<Integer, BigDecimal> actualByCategory = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();
        
        for (ExpenseTransaction transaction : transactions) {
            if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                Integer categoryId = transaction.getCategory().getId();
                categoryNames.put(categoryId, transaction.getCategory().getName());
                
                BigDecimal currentAmount = actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
                actualByCategory.put(categoryId, currentAmount.add(transaction.getAmount()));
            }
        }
        
        // Tạo danh sách so sánh
        List<CategoryComparisonDTO> comparisons = new ArrayList<>();
        
        // Thêm các danh mục có trong ngân sách
        for (ExpenseBudget budget : budgets) {
            Integer categoryId = budget.getCategory().getId();
            
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(budget.getCategory().getName());
            comparison.setBudgetAmount(budget.getAmount());
            comparison.setActualAmount(actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO));
            comparison.setDifference(comparison.getActualAmount().subtract(comparison.getBudgetAmount()));
            
            // Tính % của tổng thực tế
            if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = comparison.getActualAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalActual, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                comparison.setPercentageOfTotal(percentage);
            } else {
                comparison.setPercentageOfTotal(0.0);
            }
            
            comparisons.add(comparison);
            
            // Xóa khỏi map để theo dõi những gì đã được xử lý
            actualByCategory.remove(categoryId);
        }
        
        // Thêm các danh mục có giao dịch thực tế nhưng không có trong ngân sách
        for (Map.Entry<Integer, BigDecimal> entry : actualByCategory.entrySet()) {
            Integer categoryId = entry.getKey();
            BigDecimal amount = entry.getValue();
            
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(categoryNames.get(categoryId));
            comparison.setBudgetAmount(BigDecimal.ZERO);
            comparison.setActualAmount(amount);
            comparison.setDifference(amount);
            
            // Tính % của tổng thực tế
            if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = amount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalActual, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                comparison.setPercentageOfTotal(percentage);
            } else {
                comparison.setPercentageOfTotal(0.0);
            }
            
            comparisons.add(comparison);
        }
        
        // Sắp xếp theo số tiền thực tế giảm dần
        comparisons.sort((a, b) -> b.getActualAmount().compareTo(a.getActualAmount()));
        
        return comparisons;
    }
}
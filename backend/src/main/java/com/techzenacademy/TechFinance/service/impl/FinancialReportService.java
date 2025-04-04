package com.techzenacademy.TechFinance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.dto.report.CashFlowReportDTO;
import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO;
import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO.CategoryComparisonDTO;
import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO.ReportSummaryDTO;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;

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
        
        // Lấy tất cả giao dịch trong khoảng thời gian
        List<IncomeTransaction> incomeTransactions = incomeTransactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        List<ExpenseTransaction> expenseTransactions = expenseTransactionRepository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        // 1. Tính kế hoạch thu nhập (tất cả giao dịch)
        BigDecimal totalIncomeBudget = incomeTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 2. Tính kế hoạch chi tiêu (tất cả giao dịch)
        BigDecimal totalExpenseBudget = expenseTransactions.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 3. Tính thu nhập thực tế (chỉ tính đã thanh toán)
        BigDecimal totalIncomeActual = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 4. Tính chi tiêu thực tế (chỉ tính đã thanh toán)
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
        report.setIncomeCategories(generateIncomeCategoryComparisons(incomeTransactions));
        
        // 7. Chi tiết theo danh mục chi tiêu
        report.setExpenseCategories(generateExpenseCategoryComparisons(expenseTransactions));
        
        return report;
    }
    
    /**
     * Tạo báo cáo tài chính quý (theo từng tháng)
     */
    public List<MonthlyReportDTO> generateQuarterlyReport(Integer year, Integer quarter) {
        List<MonthlyReportDTO> reports = new ArrayList<>();
        
        // Xác định tháng bắt đầu và kết thúc của quý
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = quarter * 3;
        
        // Tạo báo cáo cho mỗi tháng trong quý
        for (int month = startMonth; month <= endMonth; month++) {
            try {
                // Mỗi báo cáo tháng đã bao gồm đầy đủ chi tiết danh mục
                MonthlyReportDTO monthlyReport = generateMonthlyReport(year, month);
                reports.add(monthlyReport);
            } catch (Exception e) {
                // Nếu không có dữ liệu cho tháng, tạo báo cáo trống
                MonthlyReportDTO emptyReport = createEmptyMonthlyReport(year, month);
                reports.add(emptyReport);
            }
        }
        
        return reports;
    }
    
    /**
     * Tạo báo cáo tài chính năm (theo từng tháng)
     */
    public List<MonthlyReportDTO> generateYearlyReport(Integer year) {
        List<MonthlyReportDTO> reports = new ArrayList<>();
        
        // Tạo báo cáo cho mỗi tháng trong năm
        for (int month = 1; month <= 12; month++) {
            try {
                MonthlyReportDTO monthlyReport = generateMonthlyReport(year, month);
                reports.add(monthlyReport);
            } catch (Exception e) {
                // Nếu không có dữ liệu cho tháng, tạo báo cáo trống
                MonthlyReportDTO emptyReport = createEmptyMonthlyReport(year, month);
                reports.add(emptyReport);
            }
        }
        
        return reports;
    }
    
    /**
     * Helper method để tạo báo cáo trống cho tháng không có dữ liệu
     */
    private MonthlyReportDTO createEmptyMonthlyReport(Integer year, Integer month) {
        MonthlyReportDTO report = new MonthlyReportDTO();
        report.setYear(year);
        report.setMonth(month);
        
        ReportSummaryDTO summary = new ReportSummaryDTO();
        summary.setTotalIncomeBudget(BigDecimal.ZERO);
        summary.setTotalIncomeActual(BigDecimal.ZERO);
        summary.setTotalExpenseBudget(BigDecimal.ZERO);
        summary.setTotalExpenseActual(BigDecimal.ZERO);
        report.setSummary(summary);
        
        report.setIncomeCategories(new ArrayList<>());
        report.setExpenseCategories(new ArrayList<>());
        
        return report;
    }
    
    /**
     * Tạo danh sách so sánh theo danh mục thu nhập
     */
    private List<CategoryComparisonDTO> generateIncomeCategoryComparisons(List<IncomeTransaction> transactions) {
        // Tạo map để tính tổng theo danh mục (kế hoạch - tất cả giao dịch)
        Map<Integer, BigDecimal> budgetByCategory = new HashMap<>();
        // Tạo map để tính tổng thực tế theo danh mục (chỉ giao dịch đã thanh toán)
        Map<Integer, BigDecimal> actualByCategory = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();
        
        // Tính toán kế hoạch (tất cả giao dịch)
        for (IncomeTransaction transaction : transactions) {
            Integer categoryId = transaction.getCategory().getId();
            categoryNames.put(categoryId, transaction.getCategory().getName());
            
            BigDecimal currentAmount = budgetByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
            budgetByCategory.put(categoryId, currentAmount.add(transaction.getAmount()));
            
            // Nếu đã thanh toán, cập nhật cả thực tế
            if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                BigDecimal currentActual = actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
                actualByCategory.put(categoryId, currentActual.add(transaction.getAmount()));
            }
        }
        
        // Tính tổng thực tế để tính phần trăm
        BigDecimal totalActual = actualByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Tạo danh sách so sánh
        List<CategoryComparisonDTO> comparisons = new ArrayList<>();
        
        // Thêm các danh mục
        for (Integer categoryId : budgetByCategory.keySet()) {
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(categoryNames.get(categoryId));
            comparison.setBudgetAmount(budgetByCategory.get(categoryId));
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
        }
        
        // Sắp xếp theo số tiền thực tế giảm dần
        comparisons.sort((a, b) -> b.getActualAmount().compareTo(a.getActualAmount()));
        
        return comparisons;
    }
    
    /**
     * Tạo danh sách so sánh theo danh mục chi tiêu
     */
    private List<CategoryComparisonDTO> generateExpenseCategoryComparisons(List<ExpenseTransaction> transactions) {
        // Tương tự như phương thức trên, nhưng cho chi tiêu
        // Tạo map để tính tổng theo danh mục (kế hoạch - tất cả giao dịch)
        Map<Integer, BigDecimal> budgetByCategory = new HashMap<>();
        // Tạo map để tính tổng thực tế theo danh mục (chỉ giao dịch đã thanh toán)
        Map<Integer, BigDecimal> actualByCategory = new HashMap<>();
        Map<Integer, String> categoryNames = new HashMap<>();
        
        // Tính toán kế hoạch (tất cả giao dịch)
        for (ExpenseTransaction transaction : transactions) {
            Integer categoryId = transaction.getCategory().getId();
            categoryNames.put(categoryId, transaction.getCategory().getName());
            
            BigDecimal currentAmount = budgetByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
            budgetByCategory.put(categoryId, currentAmount.add(transaction.getAmount()));
            
            // Nếu đã thanh toán, cập nhật cả thực tế
            if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                BigDecimal currentActual = actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
                actualByCategory.put(categoryId, currentActual.add(transaction.getAmount()));
            }
        }
        
        // Tính tổng thực tế để tính phần trăm
        BigDecimal totalActual = actualByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<CategoryComparisonDTO> comparisons = new ArrayList<>();
        
        // Thêm các danh mục
        for (Integer categoryId : budgetByCategory.keySet()) {
            CategoryComparisonDTO comparison = new CategoryComparisonDTO();
            comparison.setCategoryId(categoryId);
            comparison.setCategoryName(categoryNames.get(categoryId));
            comparison.setBudgetAmount(budgetByCategory.get(categoryId));
            comparison.setActualAmount(actualByCategory.getOrDefault(categoryId, BigDecimal.ZERO));
            comparison.setDifference(comparison.getActualAmount().subtract(comparison.getBudgetAmount()));
            
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
        }
        
        comparisons.sort((a, b) -> b.getActualAmount().compareTo(a.getActualAmount()));
        
        return comparisons;
    }

    /**
     * Tạo báo cáo dòng tiền theo năm
     */
    public CashFlowReportDTO generateCashFlowReport(Integer year) {
        CashFlowReportDTO report = new CashFlowReportDTO();
        report.setYear(year);
        
        List<CashFlowReportDTO.MonthlyDataDTO> monthlyDataList = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO; // Số dư cộng dồn
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        // Tạo dữ liệu cho từng tháng trong năm
        for (int month = 1; month <= 12; month++) {
            CashFlowReportDTO.MonthlyDataDTO monthData = new CashFlowReportDTO.MonthlyDataDTO();
            monthData.setMonth(month);
            
            // Khoảng thời gian của tháng
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
            
            // Lấy tất cả giao dịch thu nhập và chi phí đã thanh toán trong tháng
            List<IncomeTransaction> incomeTransactions = incomeTransactionRepository
                    .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate)
                    .stream()
                    .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                    .collect(Collectors.toList());
            
            List<ExpenseTransaction> expenseTransactions = expenseTransactionRepository
                    .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate)
                    .stream()
                    .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID)
                    .collect(Collectors.toList());
            
            // Tính tổng thu nhập và chi phí của tháng
            BigDecimal monthlyIncome = incomeTransactions.stream()
                    .map(IncomeTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal monthlyExpense = expenseTransactions.stream()
                    .map(ExpenseTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Tính lợi nhuận/lỗ của tháng
            BigDecimal monthlyProfit = monthlyIncome.subtract(monthlyExpense);
            
            // Cập nhật số dư cộng dồn
            runningBalance = runningBalance.add(monthlyProfit);
            
            // Cập nhật tổng thu nhập và chi phí
            totalIncome = totalIncome.add(monthlyIncome);
            totalExpense = totalExpense.add(monthlyExpense);
            
            // Cập nhật dữ liệu tháng
            monthData.setIncome(monthlyIncome);
            monthData.setExpense(monthlyExpense);
            monthData.setProfit(monthlyProfit);
            monthData.setBalance(runningBalance);
            
            monthlyDataList.add(monthData);
        }
        
        // Tạo tổng kết
        CashFlowReportDTO.SummaryDTO summary = new CashFlowReportDTO.SummaryDTO();
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setTotalProfit(totalIncome.subtract(totalExpense));
        summary.setEndBalance(runningBalance);
        
        report.setSummary(summary);
        report.setMonthlyData(monthlyDataList);
        
        return report;
    }
}
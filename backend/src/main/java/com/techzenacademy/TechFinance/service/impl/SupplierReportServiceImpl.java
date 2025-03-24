package com.techzenacademy.TechFinance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.dto.CategorySummaryDTO;
import com.techzenacademy.TechFinance.dto.SupplierReportDTO;
import com.techzenacademy.TechFinance.dto.TransactionSummaryDTO;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.service.SupplierReportService;

@Service
public class SupplierReportServiceImpl implements SupplierReportService {

    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;

    @Override
    public Map<String, Object> generateMonthlyReport(int year, int month) {
        // Tạo phạm vi ngày tháng cho 1 tháng cụ thể
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // Lấy dữ liệu giao dịch chi tiêu trong khoảng thời gian
        List<ExpenseTransaction> transactions = expenseTransactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && 
                             !t.getTransactionDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        return generateReport(transactions);
    }

    @Override
    public Map<String, Object> generateQuarterlyReport(int year, int quarter) {
        // Tính tháng bắt đầu và kết thúc của quý
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate startDate = LocalDate.of(year, startMonth, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);
        
        // Lấy dữ liệu giao dịch chi tiêu trong khoảng thời gian
        List<ExpenseTransaction> transactions = expenseTransactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && 
                             !t.getTransactionDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        return generateReport(transactions);
    }
    
    private Map<String, Object> generateReport(List<ExpenseTransaction> transactions) {
        Map<String, Object> result = new HashMap<>();
        List<SupplierReportDTO> supplierReports = new ArrayList<>();
        
        // Tính tổng số tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        
        for (ExpenseTransaction transaction : transactions) {
            totalAmount = totalAmount.add(transaction.getAmount());
            if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                paidAmount = paidAmount.add(transaction.getAmount());
            } else {
                unpaidAmount = unpaidAmount.add(transaction.getAmount());
            }
        }
        
        // Nhóm giao dịch theo nhà cung cấp
        Map<Supplier, List<ExpenseTransaction>> transactionsBySupplier = new HashMap<>();
        for (ExpenseTransaction transaction : transactions) {
            Supplier supplier = transaction.getSupplier();
            if (supplier != null) {
                if (!transactionsBySupplier.containsKey(supplier)) {
                    transactionsBySupplier.put(supplier, new ArrayList<>());
                }
                transactionsBySupplier.get(supplier).add(transaction);
            }
        }
        
        // Tạo báo cáo cho từng nhà cung cấp
        for (Map.Entry<Supplier, List<ExpenseTransaction>> entry : transactionsBySupplier.entrySet()) {
            Supplier supplier = entry.getKey();
            List<ExpenseTransaction> supplierTransactions = entry.getValue();
            
            SupplierReportDTO report = new SupplierReportDTO();
            report.setSupplierId(supplier.getId());
            report.setSupplierName(supplier.getName());
            report.setEmail(supplier.getEmail());
            report.setPhone(supplier.getPhone());
            
            // Tính tổng số tiền giao dịch của nhà cung cấp
            BigDecimal supplierTotal = BigDecimal.ZERO;
            BigDecimal supplierPaid = BigDecimal.ZERO;
            BigDecimal supplierUnpaid = BigDecimal.ZERO;
            
            for (ExpenseTransaction transaction : supplierTransactions) {
                supplierTotal = supplierTotal.add(transaction.getAmount());
                if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                    supplierPaid = supplierPaid.add(transaction.getAmount());
                } else {
                    supplierUnpaid = supplierUnpaid.add(transaction.getAmount());
                }
            }
            
            report.setTotalAmount(supplierTotal);
            report.setPaidAmount(supplierPaid);
            report.setUnpaidAmount(supplierUnpaid);
            
            // Tính phần trăm đóng góp vào tổng chi phí
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = supplierTotal
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                report.setPercentage(percentage);
            } else {
                report.setPercentage(BigDecimal.ZERO);
            }
            
            // Tạo thống kê theo tháng
            report.setTransactionsByMonth(generateMonthlyTransactionSummary(supplierTransactions));
            
            // Tạo thống kê theo danh mục
            report.setTransactionsByCategory(generateCategorySummary(supplierTransactions));
            
            supplierReports.add(report);
        }
        
        // Sắp xếp theo tổng số tiền giảm dần
        supplierReports.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        // Tạo kết quả báo cáo
        result.put("entities", supplierReports);
        result.put("totalAmount", totalAmount);
        result.put("paidAmount", paidAmount);
        result.put("unpaidAmount", unpaidAmount);
        
        return result;
    }
    
    private List<TransactionSummaryDTO> generateMonthlyTransactionSummary(List<ExpenseTransaction> transactions) {
        Map<String, List<ExpenseTransaction>> transactionsByMonth = new HashMap<>();
        
        for (ExpenseTransaction transaction : transactions) {
            LocalDate date = transaction.getTransactionDate();
            String key = date.getYear() + "-" + date.getMonthValue();
            
            if (!transactionsByMonth.containsKey(key)) {
                transactionsByMonth.put(key, new ArrayList<>());
            }
            transactionsByMonth.get(key).add(transaction);
        }
        
        List<TransactionSummaryDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<ExpenseTransaction>> entry : transactionsByMonth.entrySet()) {
            String[] yearMonth = entry.getKey().split("-");
            int year = Integer.parseInt(yearMonth[0]);
            int month = Integer.parseInt(yearMonth[1]);
            List<ExpenseTransaction> monthTransactions = entry.getValue();
            
            TransactionSummaryDTO summary = new TransactionSummaryDTO();
            summary.setYear(year);
            summary.setMonth(month);
            summary.setTransactionCount(monthTransactions.size());
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ExpenseTransaction transaction : monthTransactions) {
                totalAmount = totalAmount.add(transaction.getAmount());
            }
            summary.setAmount(totalAmount);
            
            result.add(summary);
        }
        
        // Sắp xếp theo năm và tháng
        result.sort((a, b) -> {
            if (a.getYear().equals(b.getYear())) {
                return a.getMonth().compareTo(b.getMonth());
            }
            return a.getYear().compareTo(b.getYear());
        });
        
        return result;
    }
    
    private List<CategorySummaryDTO> generateCategorySummary(List<ExpenseTransaction> transactions) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenseTransaction transaction : transactions) {
            totalAmount = totalAmount.add(transaction.getAmount());
        }
        
        Map<ExpenseCategory, List<ExpenseTransaction>> transactionsByCategory = new HashMap<>();
        
        for (ExpenseTransaction transaction : transactions) {
            ExpenseCategory category = transaction.getCategory();
            if (category != null) {
                if (!transactionsByCategory.containsKey(category)) {
                    transactionsByCategory.put(category, new ArrayList<>());
                }
                transactionsByCategory.get(category).add(transaction);
            }
        }
        
        List<CategorySummaryDTO> result = new ArrayList<>();
        
        for (Map.Entry<ExpenseCategory, List<ExpenseTransaction>> entry : transactionsByCategory.entrySet()) {
            ExpenseCategory category = entry.getKey();
            List<ExpenseTransaction> categoryTransactions = entry.getValue();
            
            CategorySummaryDTO summary = new CategorySummaryDTO();
            summary.setCategoryId(category.getId());
            summary.setCategoryName(category.getName());
            
            BigDecimal categoryAmount = BigDecimal.ZERO;
            for (ExpenseTransaction transaction : categoryTransactions) {
                categoryAmount = categoryAmount.add(transaction.getAmount());
            }
            summary.setAmount(categoryAmount);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = categoryAmount
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                summary.setPercentage(percentage);
            } else {
                summary.setPercentage(BigDecimal.ZERO);
            }
            
            result.add(summary);
        }
        
        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        
        return result;
    }
} 
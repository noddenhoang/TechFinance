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
import com.techzenacademy.TechFinance.dto.CustomerReportDTO;
import com.techzenacademy.TechFinance.dto.TransactionSummaryDTO;
import com.techzenacademy.TechFinance.entity.Customer;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import com.techzenacademy.TechFinance.service.CustomerReportService;

@Service
public class CustomerReportServiceImpl implements CustomerReportService {

    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;

    @Override
    public Map<String, Object> generateMonthlyReport(int year, int month) {
        // Tạo phạm vi ngày tháng cho 1 tháng cụ thể
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // Lấy dữ liệu giao dịch thu nhập trong khoảng thời gian
        List<IncomeTransaction> transactions = incomeTransactionRepository.findAll().stream()
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
        
        // Lấy dữ liệu giao dịch thu nhập trong khoảng thời gian
        List<IncomeTransaction> transactions = incomeTransactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && 
                             !t.getTransactionDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        return generateReport(transactions);
    }
    
    @Override
    public Map<String, Object> generateYearlyReport(int year) {
        // Tạo phạm vi ngày tháng cho cả năm
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        // Lấy dữ liệu giao dịch thu nhập trong khoảng thời gian
        List<IncomeTransaction> transactions = incomeTransactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(startDate) && 
                             !t.getTransactionDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        return generateReport(transactions);
    }
    
    private Map<String, Object> generateReport(List<IncomeTransaction> transactions) {
        Map<String, Object> result = new HashMap<>();
        List<CustomerReportDTO> customerReports = new ArrayList<>();
        
        // Tính tổng số tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        
        for (IncomeTransaction transaction : transactions) {
            totalAmount = totalAmount.add(transaction.getAmount());
            if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                receivedAmount = receivedAmount.add(transaction.getAmount());
            } else {
                pendingAmount = pendingAmount.add(transaction.getAmount());
            }
        }
        
        // Nhóm giao dịch theo khách hàng
        Map<Customer, List<IncomeTransaction>> transactionsByCustomer = new HashMap<>();
        for (IncomeTransaction transaction : transactions) {
            Customer customer = transaction.getCustomer();
            if (customer != null) {
                if (!transactionsByCustomer.containsKey(customer)) {
                    transactionsByCustomer.put(customer, new ArrayList<>());
                }
                transactionsByCustomer.get(customer).add(transaction);
            }
        }
        
        // Tạo báo cáo cho từng khách hàng
        for (Map.Entry<Customer, List<IncomeTransaction>> entry : transactionsByCustomer.entrySet()) {
            Customer customer = entry.getKey();
            List<IncomeTransaction> customerTransactions = entry.getValue();
            
            CustomerReportDTO report = new CustomerReportDTO();
            report.setCustomerId(customer.getId());
            report.setCustomerName(customer.getName());
            report.setEmail(customer.getEmail());
            report.setPhone(customer.getPhone());
            
            // Tính tổng số tiền giao dịch của khách hàng
            BigDecimal customerTotal = BigDecimal.ZERO;
            BigDecimal customerReceived = BigDecimal.ZERO;
            BigDecimal customerPending = BigDecimal.ZERO;
            
            for (IncomeTransaction transaction : customerTransactions) {
                customerTotal = customerTotal.add(transaction.getAmount());
                if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                    customerReceived = customerReceived.add(transaction.getAmount());
                } else {
                    customerPending = customerPending.add(transaction.getAmount());
                }
            }
            
            report.setTotalAmount(customerTotal);
            report.setReceivedAmount(customerReceived);
            report.setPendingAmount(customerPending);
            
            // Tính phần trăm đóng góp vào tổng thu nhập
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = customerTotal
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                report.setPercentage(percentage);
            } else {
                report.setPercentage(BigDecimal.ZERO);
            }
            
            // Tạo thống kê theo tháng
            report.setTransactionsByMonth(generateMonthlyTransactionSummary(customerTransactions));
            
            // Tạo thống kê theo danh mục
            report.setTransactionsByCategory(generateCategorySummary(customerTransactions));
            
            customerReports.add(report);
        }
        
        // Sắp xếp theo tổng số tiền giảm dần
        customerReports.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        // Tạo kết quả báo cáo
        result.put("entities", customerReports);
        result.put("totalAmount", totalAmount);
        result.put("receivedAmount", receivedAmount);
        result.put("pendingAmount", pendingAmount);
        
        return result;
    }
    
    private List<TransactionSummaryDTO> generateMonthlyTransactionSummary(List<IncomeTransaction> transactions) {
        Map<String, List<IncomeTransaction>> transactionsByMonth = new HashMap<>();
        
        for (IncomeTransaction transaction : transactions) {
            LocalDate date = transaction.getTransactionDate();
            String key = date.getYear() + "-" + date.getMonthValue();
            
            if (!transactionsByMonth.containsKey(key)) {
                transactionsByMonth.put(key, new ArrayList<>());
            }
            transactionsByMonth.get(key).add(transaction);
        }
        
        List<TransactionSummaryDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<IncomeTransaction>> entry : transactionsByMonth.entrySet()) {
            String[] yearMonth = entry.getKey().split("-");
            int year = Integer.parseInt(yearMonth[0]);
            int month = Integer.parseInt(yearMonth[1]);
            List<IncomeTransaction> monthTransactions = entry.getValue();
            
            TransactionSummaryDTO summary = new TransactionSummaryDTO();
            summary.setYear(year);
            summary.setMonth(month);
            summary.setTransactionCount(monthTransactions.size());
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (IncomeTransaction transaction : monthTransactions) {
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
    
    private List<CategorySummaryDTO> generateCategorySummary(List<IncomeTransaction> transactions) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (IncomeTransaction transaction : transactions) {
            totalAmount = totalAmount.add(transaction.getAmount());
        }
        
        Map<IncomeCategory, List<IncomeTransaction>> transactionsByCategory = new HashMap<>();
        
        for (IncomeTransaction transaction : transactions) {
            IncomeCategory category = transaction.getCategory();
            if (category != null) {
                if (!transactionsByCategory.containsKey(category)) {
                    transactionsByCategory.put(category, new ArrayList<>());
                }
                transactionsByCategory.get(category).add(transaction);
            }
        }
        
        List<CategorySummaryDTO> result = new ArrayList<>();
        
        for (Map.Entry<IncomeCategory, List<IncomeTransaction>> entry : transactionsByCategory.entrySet()) {
            IncomeCategory category = entry.getKey();
            List<IncomeTransaction> categoryTransactions = entry.getValue();
            
            CategorySummaryDTO summary = new CategorySummaryDTO();
            summary.setCategoryId(category.getId());
            summary.setCategoryName(category.getName());
            
            BigDecimal categoryAmount = BigDecimal.ZERO;
            for (IncomeTransaction transaction : categoryTransactions) {
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
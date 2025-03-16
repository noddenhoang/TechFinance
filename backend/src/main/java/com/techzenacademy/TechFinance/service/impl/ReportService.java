package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.CategorySummaryDTO;
import com.techzenacademy.TechFinance.dto.CustomerReportDTO;
import com.techzenacademy.TechFinance.dto.SupplierReportDTO;
import com.techzenacademy.TechFinance.dto.TransactionSummaryDTO;
import com.techzenacademy.TechFinance.entity.Customer;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.repository.CustomerRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import com.techzenacademy.TechFinance.repository.SupplierRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;

    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get reports for all customers with transaction summaries
     */
    public List<CustomerReportDTO> getCustomerReports(
            LocalDate startDate, LocalDate endDate, Integer year, Integer month, String sortBy) {
        
        List<Customer> customers = customerRepository.findByIsActiveTrue();
        List<CustomerReportDTO> reports = new ArrayList<>();
        
        // Get total income for percentage calculation
        BigDecimal totalIncome = getTotalIncome(startDate, endDate, year, month);
        
        for (Customer customer : customers) {
            CustomerReportDTO report = new CustomerReportDTO();
            report.setCustomerId(customer.getId());
            report.setCustomerName(customer.getName());
            report.setEmail(customer.getEmail());
            report.setPhone(customer.getPhone());
            
            // Get transaction amounts
            String jpql = "SELECT SUM(i.amount) as totalAmount, " +
                    "SUM(CASE WHEN i.paymentStatus = 'RECEIVED' THEN i.amount ELSE 0 END) as receivedAmount, " +
                    "SUM(CASE WHEN i.paymentStatus = 'PENDING' THEN i.amount ELSE 0 END) as pendingAmount " +
                    "FROM IncomeTransaction i WHERE i.customer.id = :customerId";
            
            Map<String, Object> params = new HashMap<>();
            params.put("customerId", customer.getId());
            
            jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
            
            Object[] result = (Object[]) executeQuery(jpql, params);
            
            BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            BigDecimal receivedAmount = result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO;
            BigDecimal pendingAmount = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;
            
            report.setTotalAmount(totalAmount);
            report.setReceivedAmount(receivedAmount);
            report.setPendingAmount(pendingAmount);
            
            // Calculate percentage of total
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = totalAmount
                        .multiply(new BigDecimal("100"))
                        .divide(totalIncome, 2, RoundingMode.HALF_UP);
                report.setPercentage(percentage);
            } else {
                report.setPercentage(BigDecimal.ZERO);
            }
            
            reports.add(report);
        }
        
        // Remove customers with zero transactions if not explicitly requested
        reports = reports.stream()
                .filter(report -> report.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        // Sort the reports
        sortCustomerReports(reports, sortBy);
        
        return reports;
    }

    /**
     * Get detailed report for a specific customer
     */
    public CustomerReportDTO getCustomerDetailReport(
            Integer customerId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        
        CustomerReportDTO report = new CustomerReportDTO();
        report.setCustomerId(customer.getId());
        report.setCustomerName(customer.getName());
        report.setEmail(customer.getEmail());
        report.setPhone(customer.getPhone());
        
        // Get transaction amounts
        String jpql = "SELECT SUM(i.amount) as totalAmount, " +
                "SUM(CASE WHEN i.paymentStatus = 'RECEIVED' THEN i.amount ELSE 0 END) as receivedAmount, " +
                "SUM(CASE WHEN i.paymentStatus = 'PENDING' THEN i.amount ELSE 0 END) as pendingAmount " +
                "FROM IncomeTransaction i WHERE i.customer.id = :customerId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        
        Object[] result = (Object[]) executeQuery(jpql, params);
        
        BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
        BigDecimal receivedAmount = result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO;
        BigDecimal pendingAmount = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;
        
        report.setTotalAmount(totalAmount);
        report.setReceivedAmount(receivedAmount);
        report.setPendingAmount(pendingAmount);
        
        // Calculate percentage of total
        BigDecimal totalIncome = getTotalIncome(startDate, endDate, year, month);
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalAmount
                    .multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
            report.setPercentage(percentage);
        } else {
            report.setPercentage(BigDecimal.ZERO);
        }
        
        // Get transactions by month
        report.setTransactionsByMonth(getCustomerTransactionsByMonth(customerId, startDate, endDate, year, month));
        
        // Get transactions by category
        report.setTransactionsByCategory(getCustomerTransactionsByCategory(customerId, startDate, endDate, year, month));
        
        return report;
    }

    /**
     * Get reports for all suppliers with transaction summaries
     */
    public List<SupplierReportDTO> getSupplierReports(
            LocalDate startDate, LocalDate endDate, Integer year, Integer month, String sortBy) {
        
        List<Supplier> suppliers = supplierRepository.findByIsActiveTrue();
        List<SupplierReportDTO> reports = new ArrayList<>();
        
        // Get total expense for percentage calculation
        BigDecimal totalExpense = getTotalExpense(startDate, endDate, year, month);
        
        for (Supplier supplier : suppliers) {
            SupplierReportDTO report = new SupplierReportDTO();
            report.setSupplierId(supplier.getId());
            report.setSupplierName(supplier.getName());
            report.setEmail(supplier.getEmail());
            report.setPhone(supplier.getPhone());
            
            // Get transaction amounts
            String jpql = "SELECT SUM(e.amount) as totalAmount, " +
                    "SUM(CASE WHEN e.paymentStatus = 'PAID' THEN e.amount ELSE 0 END) as paidAmount, " +
                    "SUM(CASE WHEN e.paymentStatus = 'UNPAID' THEN e.amount ELSE 0 END) as unpaidAmount " +
                    "FROM ExpenseTransaction e WHERE e.supplier.id = :supplierId";
            
            Map<String, Object> params = new HashMap<>();
            params.put("supplierId", supplier.getId());
            
            jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
            
            Object[] result = (Object[]) executeQuery(jpql, params);
            
            BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
            BigDecimal paidAmount = result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO;
            BigDecimal unpaidAmount = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;
            
            report.setTotalAmount(totalAmount);
            report.setPaidAmount(paidAmount);
            report.setUnpaidAmount(unpaidAmount);
            
            // Calculate percentage of total
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = totalAmount
                        .multiply(new BigDecimal("100"))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP);
                report.setPercentage(percentage);
            } else {
                report.setPercentage(BigDecimal.ZERO);
            }
            
            reports.add(report);
        }
        
        // Remove suppliers with zero transactions if not explicitly requested
        reports = reports.stream()
                .filter(report -> report.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        // Sort the reports
        sortSupplierReports(reports, sortBy);
        
        return reports;
    }

    /**
     * Get detailed report for a specific supplier
     */
    public SupplierReportDTO getSupplierDetailReport(
            Integer supplierId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + supplierId));
        
        SupplierReportDTO report = new SupplierReportDTO();
        report.setSupplierId(supplier.getId());
        report.setSupplierName(supplier.getName());
        report.setEmail(supplier.getEmail());
        report.setPhone(supplier.getPhone());
        
        // Get transaction amounts
        String jpql = "SELECT SUM(e.amount) as totalAmount, " +
                "SUM(CASE WHEN e.paymentStatus = 'PAID' THEN e.amount ELSE 0 END) as paidAmount, " +
                "SUM(CASE WHEN e.paymentStatus = 'UNPAID' THEN e.amount ELSE 0 END) as unpaidAmount " +
                "FROM ExpenseTransaction e WHERE e.supplier.id = :supplierId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("supplierId", supplierId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        
        Object[] result = (Object[]) executeQuery(jpql, params);
        
        BigDecimal totalAmount = result[0] != null ? (BigDecimal) result[0] : BigDecimal.ZERO;
        BigDecimal paidAmount = result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO;
        BigDecimal unpaidAmount = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;
        
        report.setTotalAmount(totalAmount);
        report.setPaidAmount(paidAmount);
        report.setUnpaidAmount(unpaidAmount);
        
        // Calculate percentage of total
        BigDecimal totalExpense = getTotalExpense(startDate, endDate, year, month);
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalAmount
                    .multiply(new BigDecimal("100"))
                    .divide(totalExpense, 2, RoundingMode.HALF_UP);
            report.setPercentage(percentage);
        } else {
            report.setPercentage(BigDecimal.ZERO);
        }
        
        // Get transactions by month
        report.setTransactionsByMonth(getSupplierTransactionsByMonth(supplierId, startDate, endDate, year, month));
        
        // Get transactions by category
        report.setTransactionsByCategory(getSupplierTransactionsByCategory(supplierId, startDate, endDate, year, month));
        
        return report;
    }

    // Helper methods

    private List<TransactionSummaryDTO> getCustomerTransactionsByMonth(Integer customerId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT YEAR(i.transactionDate) as year, MONTH(i.transactionDate) as month, " +
                "SUM(i.amount) as amount FROM IncomeTransaction i " +
                "WHERE i.customer.id = :customerId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        jpql += " GROUP BY YEAR(i.transactionDate), MONTH(i.transactionDate) ORDER BY year, month";
        
        List<Object[]> resultList = entityManager.createQuery(jpql, Object[].class)
                .setParameter("customerId", customerId)
                .getResultList();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TransactionSummaryDTO> summaries = new ArrayList<>();
        
        // Calculate total for percentage
        for (Object[] row : resultList) {
            BigDecimal amount = (BigDecimal) row[2];
            totalAmount = totalAmount.add(amount);
        }
        
        // Create DTOs
        for (Object[] row : resultList) {
            TransactionSummaryDTO summary = new TransactionSummaryDTO();
            summary.setYear((Integer) row[0]);
            summary.setMonth((Integer) row[1]);
            summary.setAmount((BigDecimal) row[2]);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = summary.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                summary.setPercentage(percentage);
            } else {
                summary.setPercentage(BigDecimal.ZERO);
            }
            
            summaries.add(summary);
        }
        
        return summaries;
    }

    private List<CategorySummaryDTO> getCustomerTransactionsByCategory(Integer customerId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT i.category.id as categoryId, i.category.name as categoryName, " +
                "SUM(i.amount) as amount FROM IncomeTransaction i " +
                "WHERE i.customer.id = :customerId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        jpql += " GROUP BY i.category.id, i.category.name ORDER BY amount DESC";
        
        List<Object[]> resultList = entityManager.createQuery(jpql, Object[].class)
                .setParameter("customerId", customerId)
                .getResultList();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CategorySummaryDTO> summaries = new ArrayList<>();
        
        // Calculate total for percentage
        for (Object[] row : resultList) {
            BigDecimal amount = (BigDecimal) row[2];
            totalAmount = totalAmount.add(amount);
        }
        
        // Create DTOs
        for (Object[] row : resultList) {
            CategorySummaryDTO summary = new CategorySummaryDTO();
            summary.setCategoryId((Integer) row[0]);
            summary.setCategoryName((String) row[1]);
            summary.setAmount((BigDecimal) row[2]);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = summary.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                summary.setPercentage(percentage);
            } else {
                summary.setPercentage(BigDecimal.ZERO);
            }
            
            summaries.add(summary);
        }
        
        return summaries;
    }

    private List<TransactionSummaryDTO> getSupplierTransactionsByMonth(Integer supplierId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT YEAR(e.transactionDate) as year, MONTH(e.transactionDate) as month, " +
                "SUM(e.amount) as amount FROM ExpenseTransaction e " +
                "WHERE e.supplier.id = :supplierId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("supplierId", supplierId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        jpql += " GROUP BY YEAR(e.transactionDate), MONTH(e.transactionDate) ORDER BY year, month";
        
        List<Object[]> resultList = entityManager.createQuery(jpql, Object[].class)
                .setParameter("supplierId", supplierId)
                .getResultList();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TransactionSummaryDTO> summaries = new ArrayList<>();
        
        // Calculate total for percentage
        for (Object[] row : resultList) {
            BigDecimal amount = (BigDecimal) row[2];
            totalAmount = totalAmount.add(amount);
        }
        
        // Create DTOs
        for (Object[] row : resultList) {
            TransactionSummaryDTO summary = new TransactionSummaryDTO();
            summary.setYear((Integer) row[0]);
            summary.setMonth((Integer) row[1]);
            summary.setAmount((BigDecimal) row[2]);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = summary.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                summary.setPercentage(percentage);
            } else {
                summary.setPercentage(BigDecimal.ZERO);
            }
            
            summaries.add(summary);
        }
        
        return summaries;
    }

    private List<CategorySummaryDTO> getSupplierTransactionsByCategory(Integer supplierId, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT e.category.id as categoryId, e.category.name as categoryName, " +
                "SUM(e.amount) as amount FROM ExpenseTransaction e " +
                "WHERE e.supplier.id = :supplierId";
        
        Map<String, Object> params = new HashMap<>();
        params.put("supplierId", supplierId);
        
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        jpql += " GROUP BY e.category.id, e.category.name ORDER BY amount DESC";
        
        List<Object[]> resultList = entityManager.createQuery(jpql, Object[].class)
                .setParameter("supplierId", supplierId)
                .getResultList();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CategorySummaryDTO> summaries = new ArrayList<>();
        
        // Calculate total for percentage
        for (Object[] row : resultList) {
            BigDecimal amount = (BigDecimal) row[2];
            totalAmount = totalAmount.add(amount);
        }
        
        // Create DTOs
        for (Object[] row : resultList) {
            CategorySummaryDTO summary = new CategorySummaryDTO();
            summary.setCategoryId((Integer) row[0]);
            summary.setCategoryName((String) row[1]);
            summary.setAmount((BigDecimal) row[2]);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = summary.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                summary.setPercentage(percentage);
            } else {
                summary.setPercentage(BigDecimal.ZERO);
            }
            
            summaries.add(summary);
        }
        
        return summaries;
    }

    private BigDecimal getTotalIncome(LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT SUM(i.amount) FROM IncomeTransaction i WHERE 1=1";
        Map<String, Object> params = new HashMap<>();
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        
        BigDecimal result = (BigDecimal) executeQuery(jpql, params);
        return result != null ? result : BigDecimal.ZERO;
    }

    private BigDecimal getTotalExpense(LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        String jpql = "SELECT SUM(e.amount) FROM ExpenseTransaction e WHERE 1=1";
        Map<String, Object> params = new HashMap<>();
        jpql = appendDateFilters(jpql, params, startDate, endDate, year, month);
        
        BigDecimal result = (BigDecimal) executeQuery(jpql, params);
        return result != null ? result : BigDecimal.ZERO;
    }

    private void sortCustomerReports(List<CustomerReportDTO> reports, String sortBy) {
        Comparator<CustomerReportDTO> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(CustomerReportDTO::getCustomerName);
                break;
            case "pending":
                comparator = Comparator.comparing(CustomerReportDTO::getPendingAmount).reversed();
                break;
            case "received":
                comparator = Comparator.comparing(CustomerReportDTO::getReceivedAmount).reversed();
                break;
            case "percentage":
                comparator = Comparator.comparing(CustomerReportDTO::getPercentage).reversed();
                break;
            case "total":
            case "amount":
            default:
                comparator = Comparator.comparing(CustomerReportDTO::getTotalAmount).reversed();
                break;
        }
        
        reports.sort(comparator);
    }

    private void sortSupplierReports(List<SupplierReportDTO> reports, String sortBy) {
        Comparator<SupplierReportDTO> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(SupplierReportDTO::getSupplierName);
                break;
            case "unpaid":
                comparator = Comparator.comparing(SupplierReportDTO::getUnpaidAmount).reversed();
                break;
            case "paid":
                comparator = Comparator.comparing(SupplierReportDTO::getPaidAmount).reversed();
                break;
            case "percentage":
                comparator = Comparator.comparing(SupplierReportDTO::getPercentage).reversed();
                break;
            case "total":
            case "amount":
            default:
                comparator = Comparator.comparing(SupplierReportDTO::getTotalAmount).reversed();
                break;
        }
        
        reports.sort(comparator);
    }

    private String appendDateFilters(String jpql, Map<String, Object> params, 
                                   LocalDate startDate, LocalDate endDate, 
                                   Integer year, Integer month) {
        if (startDate != null && endDate != null) {
            jpql += " AND transactionDate BETWEEN :startDate AND :endDate";
            params.put("startDate", startDate);
            params.put("endDate", endDate);
        } else {
            if (year != null) {
                jpql += " AND YEAR(transactionDate) = :year";
                params.put("year", year);
            }
            
            if (month != null) {
                jpql += " AND MONTH(transactionDate) = :month";
                params.put("month", month);
            }
        }
        
        return jpql;
    }

    private Object executeQuery(String jpql, Map<String, Object> params) {
        jakarta.persistence.Query query = entityManager.createQuery(jpql);
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        return query.getSingleResult();
    }
}
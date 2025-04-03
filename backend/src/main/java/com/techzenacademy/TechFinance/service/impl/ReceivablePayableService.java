package com.techzenacademy.TechFinance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.dto.AgedPayableDTO;
import com.techzenacademy.TechFinance.dto.AgedReceivableDTO;
import com.techzenacademy.TechFinance.dto.PayableDetailDTO;
import com.techzenacademy.TechFinance.dto.ReceivableDetailDTO;
import com.techzenacademy.TechFinance.dto.ReceivablePayableDTO;
import com.techzenacademy.TechFinance.dto.ReceivablePayableReportDTO;
import com.techzenacademy.TechFinance.dto.TransactionErrorDTO;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ReceivablePayableService {

    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;

    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves simplified receivable and payable summary data for chart display
     * @param year Optional year filter
     * @param month Optional month filter
     * @return DTO containing receivable and payable data for charts
     */
    public ReceivablePayableDTO getReceivablePayableChartData(Integer year, Integer month) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        
        // Set date filters if year is provided
        if (year != null) {
            if (month != null) {
                // Year and month specified
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.plusMonths(1).minusDays(1);
            } else {
                // Only year specified
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
        }
        
        // Get transactions based on date filters
        List<IncomeTransaction> incomeTransactions;
        List<ExpenseTransaction> expenseTransactions;
        
        if (startDate != null && endDate != null) {
            incomeTransactions = incomeTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
            expenseTransactions = expenseTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        } else {
            // Get all transactions if no date filter
            incomeTransactions = incomeTransactionRepository.findAll();
            expenseTransactions = expenseTransactionRepository.findAll();
        }
        
        // Calculate receivable summaries
        BigDecimal totalReceived = incomeTransactions.stream()
                .filter(it -> it.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalPending = incomeTransactions.stream()
                .filter(it -> it.getPaymentStatus() == IncomeTransaction.PaymentStatus.PENDING)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate payable summaries
        BigDecimal totalPaid = expenseTransactions.stream()
                .filter(et -> et.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID)
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalUnpaid = expenseTransactions.stream()
                .filter(et -> et.getPaymentStatus() == ExpenseTransaction.PaymentStatus.UNPAID)
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate monthly data for charts
        List<ReceivablePayableDTO.MonthlyDataDTO> receivablesByMonth = calculateMonthlyChartData(incomeTransactions, true);
        List<ReceivablePayableDTO.MonthlyDataDTO> payablesByMonth = calculateMonthlyChartData(expenseTransactions, false);
        
        // Build the response DTO
        return ReceivablePayableDTO.builder()
                .totalReceived(totalReceived)
                .totalPending(totalPending)
                .totalReceivable(totalReceived.add(totalPending))
                .totalPaid(totalPaid)
                .totalUnpaid(totalUnpaid)
                .totalPayable(totalPaid.add(totalUnpaid))
                .receivablesByMonth(receivablesByMonth)
                .payablesByMonth(payablesByMonth)
                .build();
    }
    
    private <T> List<ReceivablePayableDTO.MonthlyDataDTO> calculateMonthlyChartData(List<T> transactions, boolean isIncome) {
        // Map to store monthly totals
        Map<Integer, BigDecimal> monthlyAmounts = new HashMap<>();
        
        // Initialize all months with zero
        for (int i = 1; i <= 12; i++) {
            monthlyAmounts.put(i, BigDecimal.ZERO);
        }
        
        // Calculate totals by month
        if (isIncome) {
            for (T transaction : transactions) {
                IncomeTransaction it = (IncomeTransaction) transaction;
                if (it.getPaymentStatus() == IncomeTransaction.PaymentStatus.PENDING) {
                    int month = it.getTransactionDate().getMonthValue();
                    BigDecimal currentAmount = monthlyAmounts.get(month);
                    monthlyAmounts.put(month, currentAmount.add(it.getAmount()));
                }
            }
        } else {
            for (T transaction : transactions) {
                ExpenseTransaction et = (ExpenseTransaction) transaction;
                if (et.getPaymentStatus() == ExpenseTransaction.PaymentStatus.UNPAID) {
                    int month = et.getTransactionDate().getMonthValue();
                    BigDecimal currentAmount = monthlyAmounts.get(month);
                    monthlyAmounts.put(month, currentAmount.add(et.getAmount()));
                }
            }
        }
        
        // Convert map to list of DTOs
        return monthlyAmounts.entrySet().stream()
                .map(entry -> ReceivablePayableDTO.MonthlyDataDTO.builder()
                        .month(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy báo cáo tiền phải thu, phải trả theo tháng và năm
     */
    public ReceivablePayableReportDTO getReceivablePayableReport(Integer year, Integer month) {
        // Validate input
        if (year < 2000 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid year or month");
        }

        ReceivablePayableReportDTO report = new ReceivablePayableReportDTO();
        report.setYear(year);
        report.setMonth(month);

        // Calculate start and end dates of the month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // Get transactions for the month
        List<IncomeTransaction> incomeTransactions = 
                incomeTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        List<ExpenseTransaction> expenseTransactions = 
                expenseTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);

        // Calculate receivables (pending income)
        List<IncomeTransaction> pendingIncomes = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.PENDING)
                .collect(Collectors.toList());
                
        BigDecimal totalReceivables = pendingIncomes.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate payables (unpaid expenses)
        List<ExpenseTransaction> unpaidExpenses = expenseTransactions.stream()
                .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.UNPAID)
                .collect(Collectors.toList());
                
        BigDecimal totalPayables = unpaidExpenses.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate received income and paid expenses
        BigDecimal receivedIncome = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal paidExpense = expenseTransactions.stream()
                .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID)
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total income and expense
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalExpense = expenseTransactions.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Set summary information
        report.setTotalReceivables(totalReceivables);
        report.setTotalPayables(totalPayables);
        report.setNetWorkingCapitalImpact(totalReceivables.subtract(totalPayables));
        report.setActualProfit(receivedIncome.subtract(paidExpense));
        report.setPotentialProfit(totalIncome.subtract(totalExpense));

        // Generate detailed reports
        report.setTopReceivables(getTopReceivables(pendingIncomes, 10));
        report.setTopPayables(getTopPayables(unpaidExpenses, 10));
        report.setAgedReceivables(getAgedReceivables(pendingIncomes));
        report.setAgedPayables(getAgedPayables(unpaidExpenses));
        report.setTransactionErrors(getTransactionErrors(incomeTransactions, expenseTransactions));

        return report;
    }

    /**
     * Lấy báo cáo tiền phải thu, phải trả theo năm
     */
    public List<ReceivablePayableReportDTO> getYearlyReceivablePayableReports(Integer year) {
        // Validate year
        if (year < 2000) {
            throw new IllegalArgumentException("Invalid year");
        }
        
        List<ReceivablePayableReportDTO> yearlyReports = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            try {
                ReceivablePayableReportDTO monthReport = getReceivablePayableReport(year, month);
                yearlyReports.add(monthReport);
            } catch (Exception e) {
                // If there's no data for a month, add an empty report
                ReceivablePayableReportDTO emptyReport = new ReceivablePayableReportDTO();
                emptyReport.setYear(year);
                emptyReport.setMonth(month);
                emptyReport.setTotalReceivables(BigDecimal.ZERO);
                emptyReport.setTotalPayables(BigDecimal.ZERO);
                emptyReport.setNetWorkingCapitalImpact(BigDecimal.ZERO);
                emptyReport.setActualProfit(BigDecimal.ZERO);
                emptyReport.setPotentialProfit(BigDecimal.ZERO);
                emptyReport.setTopReceivables(new ArrayList<>());
                emptyReport.setTopPayables(new ArrayList<>());
                emptyReport.setAgedReceivables(new ArrayList<>());
                emptyReport.setAgedPayables(new ArrayList<>());
                emptyReport.setTransactionErrors(new ArrayList<>());
                yearlyReports.add(emptyReport);
            }
        }
        
        return yearlyReports;
    }

    /**
     * Lấy báo cáo tiền phải thu, phải trả theo khoảng thời gian
     */
    public ReceivablePayableReportDTO getReceivablePayableReportByDateRange(LocalDate startDate, LocalDate endDate) {
        // Validate input
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        ReceivablePayableReportDTO report = new ReceivablePayableReportDTO();
        
        // Get transactions for the period
        List<IncomeTransaction> incomeTransactions = 
                incomeTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        List<ExpenseTransaction> expenseTransactions = 
                expenseTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);

        // Calculate receivables (pending income)
        List<IncomeTransaction> pendingIncomes = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.PENDING)
                .collect(Collectors.toList());
                
        BigDecimal totalReceivables = pendingIncomes.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate payables (unpaid expenses)
        List<ExpenseTransaction> unpaidExpenses = expenseTransactions.stream()
                .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.UNPAID)
                .collect(Collectors.toList());
                
        BigDecimal totalPayables = unpaidExpenses.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate received income and paid expenses
        BigDecimal receivedIncome = incomeTransactions.stream()
                .filter(t -> t.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED)
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal paidExpense = expenseTransactions.stream()
                .filter(t -> t.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID)
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total income and expense
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalExpense = expenseTransactions.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Set summary information
        report.setTotalReceivables(totalReceivables);
        report.setTotalPayables(totalPayables);
        report.setNetWorkingCapitalImpact(totalReceivables.subtract(totalPayables));
        report.setActualProfit(receivedIncome.subtract(paidExpense));
        report.setPotentialProfit(totalIncome.subtract(totalExpense));

        // Generate detailed reports
        report.setTopReceivables(getTopReceivables(pendingIncomes, 10));
        report.setTopPayables(getTopPayables(unpaidExpenses, 10));
        report.setAgedReceivables(getAgedReceivables(pendingIncomes));
        report.setAgedPayables(getAgedPayables(unpaidExpenses));
        report.setTransactionErrors(getTransactionErrors(incomeTransactions, expenseTransactions));

        return report;
    }

    /**
     * Lấy danh sách top khoản phải thu
     */
    private List<ReceivableDetailDTO> getTopReceivables(List<IncomeTransaction> pendingIncomes, int limit) {
        return pendingIncomes.stream()
                .sorted(Comparator.comparing(IncomeTransaction::getAmount).reversed())
                .limit(limit)
                .map(this::mapToReceivableDetail)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách top khoản phải trả
     */
    private List<PayableDetailDTO> getTopPayables(List<ExpenseTransaction> unpaidExpenses, int limit) {
        return unpaidExpenses.stream()
                .sorted(Comparator.comparing(ExpenseTransaction::getAmount).reversed())
                .limit(limit)
                .map(this::mapToPayableDetail)
                .collect(Collectors.toList());
    }

    /**
     * Phân tích tuổi nợ các khoản phải thu
     */
    private List<AgedReceivableDTO> getAgedReceivables(List<IncomeTransaction> pendingIncomes) {
        LocalDate today = LocalDate.now();
        
        // Group by aging buckets
        Map<String, BigDecimal> agingBuckets = new HashMap<>();
        agingBuckets.put("0-30 ngày", BigDecimal.ZERO);
        agingBuckets.put("31-60 ngày", BigDecimal.ZERO);
        agingBuckets.put("61-90 ngày", BigDecimal.ZERO);
        agingBuckets.put("90+ ngày", BigDecimal.ZERO);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (IncomeTransaction transaction : pendingIncomes) {
            int daysOutstanding = Period.between(transaction.getTransactionDate(), today).getDays();
            String bucket;
            
            if (daysOutstanding <= 30) {
                bucket = "0-30 ngày";
            } else if (daysOutstanding <= 60) {
                bucket = "31-60 ngày";
            } else if (daysOutstanding <= 90) {
                bucket = "61-90 ngày";
            } else {
                bucket = "90+ ngày";
            }
            
            BigDecimal currentAmount = agingBuckets.get(bucket);
            agingBuckets.put(bucket, currentAmount.add(transaction.getAmount()));
            totalAmount = totalAmount.add(transaction.getAmount());
        }
        
        // Convert to DTOs
        List<AgedReceivableDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : agingBuckets.entrySet()) {
            AgedReceivableDTO dto = new AgedReceivableDTO();
            dto.setAgingBucket(entry.getKey());
            dto.setAmount(entry.getValue());
            
            // Calculate percentage
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = entry.getValue()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                dto.setPercentage(percentage);
            } else {
                dto.setPercentage(BigDecimal.ZERO);
            }
            
            result.add(dto);
        }
        
        return result;
    }

    /**
     * Phân tích tuổi nợ các khoản phải trả
     */
    private List<AgedPayableDTO> getAgedPayables(List<ExpenseTransaction> unpaidExpenses) {
        LocalDate today = LocalDate.now();
        
        // Group by aging buckets
        Map<String, BigDecimal> agingBuckets = new HashMap<>();
        agingBuckets.put("0-30 ngày", BigDecimal.ZERO);
        agingBuckets.put("31-60 ngày", BigDecimal.ZERO);
        agingBuckets.put("61-90 ngày", BigDecimal.ZERO);
        agingBuckets.put("90+ ngày", BigDecimal.ZERO);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (ExpenseTransaction transaction : unpaidExpenses) {
            int daysOutstanding = Period.between(transaction.getTransactionDate(), today).getDays();
            String bucket;
            
            if (daysOutstanding <= 30) {
                bucket = "0-30 ngày";
            } else if (daysOutstanding <= 60) {
                bucket = "31-60 ngày";
            } else if (daysOutstanding <= 90) {
                bucket = "61-90 ngày";
            } else {
                bucket = "90+ ngày";
            }
            
            BigDecimal currentAmount = agingBuckets.get(bucket);
            agingBuckets.put(bucket, currentAmount.add(transaction.getAmount()));
            totalAmount = totalAmount.add(transaction.getAmount());
        }
        
        // Convert to DTOs
        List<AgedPayableDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : agingBuckets.entrySet()) {
            AgedPayableDTO dto = new AgedPayableDTO();
            dto.setAgingBucket(entry.getKey());
            dto.setAmount(entry.getValue());
            
            // Calculate percentage
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = entry.getValue()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP);
                dto.setPercentage(percentage);
            } else {
                dto.setPercentage(BigDecimal.ZERO);
            }
            
            result.add(dto);
        }
        
        return result;
    }
    
    /**
     * Phát hiện lỗi giao dịch
     */
    private List<TransactionErrorDTO> getTransactionErrors(List<IncomeTransaction> incomeTransactions, 
                                                      List<ExpenseTransaction> expenseTransactions) {
        List<TransactionErrorDTO> errors = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Check for unusual high amounts in income transactions (more than 3 times average)
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(IncomeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (!incomeTransactions.isEmpty()) {
            BigDecimal averageIncome = totalIncome.divide(
                new BigDecimal(incomeTransactions.size()), 2, RoundingMode.HALF_UP);
            BigDecimal threshold = averageIncome.multiply(new BigDecimal("3"));
            
            incomeTransactions.stream()
                .filter(t -> t.getAmount().compareTo(threshold) > 0)
                .forEach(t -> {
                    TransactionErrorDTO error = new TransactionErrorDTO();
                    error.setTransactionId(t.getId());
                    error.setErrorType("UNUSUAL_AMOUNT");
                    error.setDescription("Giao dịch thu nhập có giá trị cao bất thường");
                    error.setAmount(t.getAmount());
                    error.setTransactionDate(t.getTransactionDate());
                    error.setTransactionType("INCOME");
                    error.setReferenceNo(t.getReferenceNo());
                    errors.add(error);
                });
        }
        
        // Check for unusual high amounts in expense transactions (more than 3 times average)
        BigDecimal totalExpense = expenseTransactions.stream()
                .map(ExpenseTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (!expenseTransactions.isEmpty()) {
            BigDecimal averageExpense = totalExpense.divide(
                new BigDecimal(expenseTransactions.size()), 2, RoundingMode.HALF_UP);
            BigDecimal threshold = averageExpense.multiply(new BigDecimal("3"));
            
            expenseTransactions.stream()
                .filter(t -> t.getAmount().compareTo(threshold) > 0)
                .forEach(t -> {
                    TransactionErrorDTO error = new TransactionErrorDTO();
                    error.setTransactionId(t.getId());
                    error.setErrorType("UNUSUAL_AMOUNT");
                    error.setDescription("Giao dịch chi phí có giá trị cao bất thường");
                    error.setAmount(t.getAmount());
                    error.setTransactionDate(t.getTransactionDate());
                    error.setTransactionType("EXPENSE");
                    error.setReferenceNo(t.getReferenceNo());
                    errors.add(error);
                });
        }
        
        // Check for duplicate references
        Map<String, Long> referenceMap = new HashMap<>();
        
        // Check income transactions
        incomeTransactions.stream()
            .filter(t -> t.getReferenceNo() != null && !t.getReferenceNo().isBlank())
            .forEach(t -> {
                referenceMap.put(t.getReferenceNo(), 
                    referenceMap.getOrDefault(t.getReferenceNo(), 0L) + 1);
            });
            
        referenceMap.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .forEach(e -> {
                TransactionErrorDTO error = new TransactionErrorDTO();
                error.setErrorType("DUPLICATE_REFERENCE");
                error.setDescription("Mã tham chiếu bị trùng lặp trong giao dịch thu nhập: " + e.getKey());
                error.setTransactionType("INCOME");
                error.setReferenceNo(e.getKey());
                errors.add(error);
            });
        
        // Reset and check expense transactions
        referenceMap.clear();
        expenseTransactions.stream()
            .filter(t -> t.getReferenceNo() != null && !t.getReferenceNo().isBlank())
            .forEach(t -> {
                referenceMap.put(t.getReferenceNo(), 
                    referenceMap.getOrDefault(t.getReferenceNo(), 0L) + 1);
            });
            
        referenceMap.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .forEach(e -> {
                TransactionErrorDTO error = new TransactionErrorDTO();
                error.setErrorType("DUPLICATE_REFERENCE");
                error.setDescription("Mã tham chiếu bị trùng lặp trong giao dịch chi phí: " + e.getKey());
                error.setTransactionType("EXPENSE");
                error.setReferenceNo(e.getKey());
                errors.add(error);
            });
        
        return errors;
    }

    /**
     * Chuyển đổi từ IncomeTransaction sang ReceivableDetailDTO
     */
    private ReceivableDetailDTO mapToReceivableDetail(IncomeTransaction transaction) {
        ReceivableDetailDTO dto = new ReceivableDetailDTO();
        dto.setTransactionId(transaction.getId());
        dto.setCustomerName(transaction.getCustomer() != null ? transaction.getCustomer().getName() : "Unknown");
        dto.setCategoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : "Unknown");
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setReferenceNo(transaction.getReferenceNo());
        
        // Calculate days outstanding
        LocalDate today = LocalDate.now();
        dto.setDaysOutstanding((long) Period.between(transaction.getTransactionDate(), today).getDays());
        
        return dto;
    }
    
    /**
     * Chuyển đổi từ ExpenseTransaction sang PayableDetailDTO
     */
    private PayableDetailDTO mapToPayableDetail(ExpenseTransaction transaction) {
        PayableDetailDTO dto = new PayableDetailDTO();
        dto.setTransactionId(transaction.getId());
        dto.setSupplierName(transaction.getSupplier() != null ? transaction.getSupplier().getName() : "Unknown");
        dto.setCategoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : "Unknown");
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setReferenceNo(transaction.getReferenceNo());
        
        // Calculate days outstanding
        LocalDate today = LocalDate.now();
        dto.setDaysOutstanding((long) Period.between(transaction.getTransactionDate(), today).getDays());
        
        return dto;
    }
}
package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.ExpenseTransactionDTO;
import com.techzenacademy.TechFinance.dto.ExpenseTransactionRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction.PaymentStatus;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.ExpenseCategoryRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.SupplierRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import com.techzenacademy.TechFinance.service.impl.ExpenseBudgetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseTransactionService {
    
    @Autowired
    private ExpenseTransactionRepository transactionRepository;
    
    @Autowired
    private ExpenseCategoryRepository categoryRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExpenseBudgetService expenseBudgetService;
    
    /**
     * Lấy giao dịch theo ID với phân trang (chỉ trả về 1 kết quả hoặc không có kết quả)
     */
    public PageResponse<ExpenseTransactionDTO> getTransactionById(Integer id, Pageable pageable) {
        Optional<ExpenseTransaction> transactionOpt = transactionRepository.findById(id);
        
        if (transactionOpt.isPresent()) {
            // Nếu tìm thấy, tạo page chứa một phần tử
            ExpenseTransactionDTO dto = mapToDTO(transactionOpt.get());
            Page<ExpenseTransactionDTO> page = new PageImpl<>(
                Collections.singletonList(dto), 
                pageable,
                1 // total elements
            );
            return new PageResponse<>(page);
        } else {
            // Nếu không tìm thấy, trả về page rỗng
            Page<ExpenseTransactionDTO> emptyPage = new PageImpl<>(
                Collections.emptyList(), 
                pageable,
                0 // total elements
            );
            return new PageResponse<>(emptyPage);
        }
    }
    
    /**
     * Tìm kiếm giao dịch với nhiều điều kiện lọc khác nhau và phân trang
     */
    public PageResponse<ExpenseTransactionDTO> getFilteredTransactions(
            Integer supplierId, 
            Integer categoryId, 
            LocalDate startDate, 
            LocalDate endDate, 
            BigDecimal minAmount, 
            BigDecimal maxAmount, 
            String paymentStatus,
            String referenceNo,
            Pageable pageable) {
        
        // Chuyển đổi chuỗi paymentStatus sang enum nếu được chỉ định
        PaymentStatus status = null;
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            try {
                status = PaymentStatus.valueOf(paymentStatus);
            } catch (IllegalArgumentException e) {
                // Bỏ qua nếu giá trị không hợp lệ
            }
        }
        
        // Lấy dữ liệu từ repository với các bộ lọc
        Page<ExpenseTransaction> transactionPage = transactionRepository.findByFilters(
                supplierId, categoryId, startDate, endDate, minAmount, maxAmount, 
                status, referenceNo, pageable);
        
        // Chuyển đổi trang kết quả sang DTOs
        Page<ExpenseTransactionDTO> dtoPage = transactionPage.map(this::mapToDTO);
        
        // Trả về đối tượng PageResponse với dữ liệu đã chuyển đổi
        return new PageResponse<>(dtoPage);
    }
    
    // Giữ lại tất cả các phương thức hiện có cho tương thích ngược
    
    public ExpenseTransactionDTO getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense transaction not found with id: " + id));
    }
    
    public List<ExpenseTransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseTransactionDTO> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseTransactionDTO> getTransactionsByCategory(Integer categoryId) {
        return transactionRepository.findByCategoryIdOrderByTransactionDateDesc(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseTransactionDTO> getTransactionsBySupplier(Integer supplierId) {
        return transactionRepository.findBySupplierIdOrderByTransactionDateDesc(supplierId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseTransactionDTO> getTransactionsByMonth(int year, int month) {
        return transactionRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ExpenseTransactionDTO createTransaction(ExpenseTransactionRequest request) {
        ExpenseTransaction transaction = new ExpenseTransaction();
        updateTransactionFromRequest(transaction, request);
        transaction.setCreatedBy(getCurrentUser());
        
        ExpenseTransaction savedTransaction = transactionRepository.save(transaction);
        
        expenseBudgetService.refreshBudgets(
            transaction.getTransactionDate().getYear(),
            transaction.getTransactionDate().getMonthValue()
        );
        
        return mapToDTO(savedTransaction);
    }
    
    @Transactional
    public ExpenseTransactionDTO updateTransaction(Integer id, ExpenseTransactionRequest request) {
        ExpenseTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense transaction not found with id: " + id));
        
        int oldYear = transaction.getTransactionDate().getYear();
        int oldMonth = transaction.getTransactionDate().getMonthValue();
        
        updateTransactionFromRequest(transaction, request);
        ExpenseTransaction updatedTransaction = transactionRepository.save(transaction);
        
        expenseBudgetService.refreshBudgets(oldYear, oldMonth);
        
        if (oldYear != updatedTransaction.getTransactionDate().getYear() || 
            oldMonth != updatedTransaction.getTransactionDate().getMonthValue()) {
            expenseBudgetService.refreshBudgets(
                updatedTransaction.getTransactionDate().getYear(), 
                updatedTransaction.getTransactionDate().getMonthValue()
            );
        }
        
        return mapToDTO(updatedTransaction);
    }
    
    @Transactional
    public void deleteTransaction(Integer id) {
        ExpenseTransaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Expense transaction not found with id: " + id));
        
        int year = transaction.getTransactionDate().getYear();
        int month = transaction.getTransactionDate().getMonthValue();
        
        transactionRepository.deleteById(id);
        
        expenseBudgetService.refreshBudgets(year, month);
    }
    
    private void updateTransactionFromRequest(ExpenseTransaction transaction, ExpenseTransactionRequest request) {
        // Only update category if provided
        if (request.getCategoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + request.getCategoryId()));
            transaction.setCategory(category);
        }
        
        // Update supplier only if explicitly provided in request
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + request.getSupplierId()));
            transaction.setSupplier(supplier);
        } else if (request.getSupplierId() == null && request.hasSupplierIdField()) {
            // Only set supplier to null if field was explicitly included in request but with null value
            transaction.setSupplier(null);
        }
        
        // Only update transaction date if provided
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
        
        // Only update amount if provided
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        
        // Only update payment status if provided
        if (request.getPaymentStatus() != null) {
            transaction.setPaymentStatus(request.getPaymentStatus());
        }
        
        // Only update description if provided
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        
        // Only update reference number if provided
        if (request.getReferenceNo() != null) {
            transaction.setReferenceNo(request.getReferenceNo());
        }
    }
    
    private ExpenseTransactionDTO mapToDTO(ExpenseTransaction transaction) {
        ExpenseTransactionDTO dto = new ExpenseTransactionDTO();
        dto.setId(transaction.getId());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setCategoryName(transaction.getCategory().getName());
        
        if (transaction.getSupplier() != null) {
            dto.setSupplierId(transaction.getSupplier().getId());
            dto.setSupplierName(transaction.getSupplier().getName());
        }
        
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setAmount(transaction.getAmount());
        dto.setPaymentStatus(transaction.getPaymentStatus());
        dto.setDescription(transaction.getDescription());
        dto.setReferenceNo(transaction.getReferenceNo());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
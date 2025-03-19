package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.IncomeTransactionDTO;
import com.techzenacademy.TechFinance.dto.IncomeTransactionRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.Customer;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.IncomeTransaction.PaymentStatus;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.CustomerRepository;
import com.techzenacademy.TechFinance.repository.IncomeCategoryRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class IncomeTransactionService {
    
    @Autowired
    private IncomeTransactionRepository transactionRepository;
    
    @Autowired
    private IncomeCategoryRepository categoryRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Lấy giao dịch theo ID với phân trang (chỉ trả về 1 kết quả hoặc không có kết quả)
     */
    public PageResponse<IncomeTransactionDTO> getTransactionById(Integer id, Pageable pageable) {
        Optional<IncomeTransaction> transactionOpt = transactionRepository.findById(id);
        
        if (transactionOpt.isPresent()) {
            // Nếu tìm thấy, tạo page chứa một phần tử
            IncomeTransactionDTO dto = mapToDTO(transactionOpt.get());
            Page<IncomeTransactionDTO> page = new PageImpl<>(
                Collections.singletonList(dto), 
                pageable,
                1 // total elements
            );
            return new PageResponse<>(page);
        } else {
            // Nếu không tìm thấy, trả về page rỗng
            Page<IncomeTransactionDTO> emptyPage = new PageImpl<>(
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
    public PageResponse<IncomeTransactionDTO> getFilteredTransactions(
            Integer customerId, 
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
        Page<IncomeTransaction> transactionPage = transactionRepository.findByFilters(
                customerId, categoryId, startDate, endDate, minAmount, maxAmount, 
                status, referenceNo, pageable);
        
        // Chuyển đổi trang kết quả sang DTOs
        Page<IncomeTransactionDTO> dtoPage = transactionPage.map(this::mapToDTO);
        
        // Trả về đối tượng PageResponse với dữ liệu đã chuyển đổi
        return new PageResponse<>(dtoPage);
    }
    
    // Giữ lại tất cả các phương thức hiện có cho tương thích ngược
    
    public IncomeTransactionDTO getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Income transaction not found with id: " + id));
    }
    
    public List<IncomeTransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeTransactionDTO> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeTransactionDTO> getTransactionsByCategory(Integer categoryId) {
        return transactionRepository.findByCategoryIdOrderByTransactionDateDesc(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeTransactionDTO> getTransactionsByCustomer(Integer customerId) {
        return transactionRepository.findByCustomerIdOrderByTransactionDateDesc(customerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeTransactionDTO> getTransactionsByMonth(int year, int month) {
        return transactionRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public IncomeTransactionDTO createTransaction(IncomeTransactionRequest request) {
        IncomeTransaction transaction = new IncomeTransaction();
        updateTransactionFromRequest(transaction, request);
        transaction.setCreatedBy(getCurrentUser());
        
        IncomeTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToDTO(savedTransaction);
    }
    
    public IncomeTransactionDTO updateTransaction(Integer id, IncomeTransactionRequest request) {
        IncomeTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Income transaction not found with id: " + id));
        
        updateTransactionFromRequest(transaction, request);
        
        IncomeTransaction updatedTransaction = transactionRepository.save(transaction);
        return mapToDTO(updatedTransaction);
    }
    
    public void deleteTransaction(Integer id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Income transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }
    
    private void updateTransactionFromRequest(IncomeTransaction transaction, IncomeTransactionRequest request) {
        // Only update category if provided
        if (request.getCategoryId() != null) {
            IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
            transaction.setCategory(category);
        }
        
        // Update customer only if explicitly provided in request
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
            transaction.setCustomer(customer);
        } else if (request.getCustomerId() == null && request.hasCustomerIdField()) {
            // Only set customer to null if field was explicitly included in request but with null value
            transaction.setCustomer(null);
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
    
    private IncomeTransactionDTO mapToDTO(IncomeTransaction transaction) {
        IncomeTransactionDTO dto = new IncomeTransactionDTO();
        dto.setId(transaction.getId());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setCategoryName(transaction.getCategory().getName());
        
        if (transaction.getCustomer() != null) {
            dto.setCustomerId(transaction.getCustomer().getId());
            dto.setCustomerName(transaction.getCustomer().getName());
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
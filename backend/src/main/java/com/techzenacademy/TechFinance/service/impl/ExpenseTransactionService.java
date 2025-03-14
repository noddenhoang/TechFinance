package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.ExpenseTransactionDTO;
import com.techzenacademy.TechFinance.dto.ExpenseTransactionRequest;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.ExpenseCategoryRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.SupplierRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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
    
    public ExpenseTransactionDTO getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense transaction not found with id: " + id));
    }
    
    public ExpenseTransactionDTO createTransaction(ExpenseTransactionRequest request) {
        ExpenseTransaction transaction = new ExpenseTransaction();
        updateTransactionFromRequest(transaction, request);
        transaction.setCreatedBy(getCurrentUser());
        
        ExpenseTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToDTO(savedTransaction);
    }
    
    public ExpenseTransactionDTO updateTransaction(Integer id, ExpenseTransactionRequest request) {
        ExpenseTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense transaction not found with id: " + id));
        
        updateTransactionFromRequest(transaction, request);
        
        ExpenseTransaction updatedTransaction = transactionRepository.save(transaction);
        return mapToDTO(updatedTransaction);
    }
    
    public void deleteTransaction(Integer id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Expense transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }
    
    private void updateTransactionFromRequest(ExpenseTransaction transaction, ExpenseTransactionRequest request) {
        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + request.getCategoryId()));
        transaction.setCategory(category);
        
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + request.getSupplierId()));
            transaction.setSupplier(supplier);
        } else {
            transaction.setSupplier(null);
        }
        
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentStatus(request.getPaymentStatus());
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNo(request.getReferenceNo());
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
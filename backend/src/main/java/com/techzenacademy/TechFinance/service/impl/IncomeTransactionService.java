package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.IncomeTransactionDTO;
import com.techzenacademy.TechFinance.dto.IncomeTransactionRequest;
import com.techzenacademy.TechFinance.entity.Customer;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.CustomerRepository;
import com.techzenacademy.TechFinance.repository.IncomeCategoryRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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
    
    public IncomeTransactionDTO getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Income transaction not found with id: " + id));
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
        IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
        transaction.setCategory(category);
        
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
            transaction.setCustomer(customer);
        } else {
            transaction.setCustomer(null);
        }
        
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentStatus(request.getPaymentStatus());
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNo(request.getReferenceNo());
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
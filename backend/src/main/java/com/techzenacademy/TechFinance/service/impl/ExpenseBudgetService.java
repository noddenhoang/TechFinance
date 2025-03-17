package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.ExpenseBudgetDTO;
import com.techzenacademy.TechFinance.dto.ExpenseBudgetRequest;
import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseCategoryRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseBudgetService {
    
    @Autowired
    private ExpenseBudgetRepository budgetRepository;
    
    @Autowired
    private ExpenseCategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<ExpenseBudgetDTO> getAllBudgets() {
        return budgetRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByYearAndMonth(Integer year, Integer month) {
        return budgetRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByYear(Integer year) {
        return budgetRepository.findByYear(year).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByCategory(Integer categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + categoryId));
                
        return budgetRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public ExpenseBudgetDTO getBudgetById(Integer id) {
        return budgetRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense budget not found with id: " + id));
    }
    
    public ExpenseBudgetDTO getBudgetByCategoryAndPeriod(Integer categoryId, Integer year, Integer month) {
        return budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Expense budget not found for category id: %d, year: %d, month: %d", 
                                  categoryId, year, month)));
    }
    
    public ExpenseBudgetDTO createBudget(ExpenseBudgetRequest request) {
        // Check if budget already exists for this category, year and month
        if (budgetRepository.existsByCategoryIdAndYearAndMonth(
                request.getCategoryId(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException(
                String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                              request.getCategoryId(), request.getYear(), request.getMonth()));
        }
        
        ExpenseBudget budget = new ExpenseBudget();
        updateBudgetFromRequest(budget, request);
        budget.setCreatedBy(getCurrentUser());
        
        ExpenseBudget savedBudget = budgetRepository.save(budget);
        return mapToDTO(savedBudget);
    }
    
    public ExpenseBudgetDTO updateBudget(Integer id, ExpenseBudgetRequest request) {
        ExpenseBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense budget not found with id: " + id));
        
        // Check if we need to check for duplicate (category, year, month) combination
        boolean needsUniqueCheck = false;
        
        if (request.getCategoryId() != null && !budget.getCategory().getId().equals(request.getCategoryId())) {
            needsUniqueCheck = true;
        }
        
        if (request.getYear() != null && !budget.getYear().equals(request.getYear())) {
            needsUniqueCheck = true;
        }
        
        if (request.getMonth() != null && !budget.getMonth().equals(request.getMonth())) {
            needsUniqueCheck = true;
        }
        
        // If category, year or month changed, check for duplicates
        if (needsUniqueCheck) {
            Integer categoryId = request.getCategoryId() != null ? request.getCategoryId() : budget.getCategory().getId();
            Integer year = request.getYear() != null ? request.getYear() : budget.getYear();
            Integer month = request.getMonth() != null ? request.getMonth() : budget.getMonth();
            
            if (budgetRepository.existsByCategoryIdAndYearAndMonth(categoryId, year, month)) {
                throw new IllegalArgumentException(
                    String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                                  categoryId, year, month));
            }
        }
        
        // Update only provided fields
        if (request.getCategoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + request.getCategoryId()));
            budget.setCategory(category);
        }
        
        if (request.getYear() != null) {
            budget.setYear(request.getYear());
        }
        
        if (request.getMonth() != null) {
            budget.setMonth(request.getMonth());
        }
        
        if (request.getAmount() != null) {
            budget.setAmount(request.getAmount());
        }
        
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        ExpenseBudget updatedBudget = budgetRepository.save(budget);
        return mapToDTO(updatedBudget);
    }
    
    public void deleteBudget(Integer id) {
        if (!budgetRepository.existsById(id)) {
            throw new EntityNotFoundException("Expense budget not found with id: " + id);
        }
        budgetRepository.deleteById(id);
    }
    
    private void updateBudgetFromRequest(ExpenseBudget budget, ExpenseBudgetRequest request) {
        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + request.getCategoryId()));
        budget.setCategory(category);
        
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());
        budget.setAmount(request.getAmount());
        budget.setNotes(request.getNotes());
    }
    
    private ExpenseBudgetDTO mapToDTO(ExpenseBudget budget) {
        ExpenseBudgetDTO dto = new ExpenseBudgetDTO();
        dto.setId(budget.getId());
        dto.setCategoryId(budget.getCategory().getId());
        dto.setCategoryName(budget.getCategory().getName());
        dto.setYear(budget.getYear());
        dto.setMonth(budget.getMonth());
        dto.setAmount(budget.getAmount());
        dto.setNotes(budget.getNotes());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
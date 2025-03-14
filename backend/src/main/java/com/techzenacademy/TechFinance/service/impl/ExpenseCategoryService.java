package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.ExpenseCategoryDTO;
import com.techzenacademy.TechFinance.dto.ExpenseCategoryRequest;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.ExpenseCategoryRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseCategoryService {
    
    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<ExpenseCategoryDTO> getAllCategories() {
        return expenseCategoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseCategoryDTO> getActiveCategories() {
        return expenseCategoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public ExpenseCategoryDTO getCategoryById(Integer id) {
        return expenseCategoryRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + id));
    }
    
    public ExpenseCategoryDTO createCategory(ExpenseCategoryRequest request) {
        if (expenseCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        
        ExpenseCategory category = new ExpenseCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        category.setCreatedBy(getCurrentUser());
        
        ExpenseCategory savedCategory = expenseCategoryRepository.save(category);
        return mapToDTO(savedCategory);
    }
    
    public ExpenseCategoryDTO updateCategory(Integer id, ExpenseCategoryRequest request) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + id));
        
        // Check if name is changed and if new name already exists
        if (!category.getName().equals(request.getName()) && 
                expenseCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        
        ExpenseCategory updatedCategory = expenseCategoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }
    
    public void deleteCategory(Integer id) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + id));
        
        // Soft delete - just mark as inactive
        category.setIsActive(false);
        expenseCategoryRepository.save(category);
    }
    
    private ExpenseCategoryDTO mapToDTO(ExpenseCategory category) {
        ExpenseCategoryDTO dto = new ExpenseCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIsActive(category.getIsActive());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
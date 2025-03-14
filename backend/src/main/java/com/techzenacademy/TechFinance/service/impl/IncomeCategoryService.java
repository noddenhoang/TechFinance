package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.IncomeCategoryDTO;
import com.techzenacademy.TechFinance.dto.IncomeCategoryRequest;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.IncomeCategoryRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncomeCategoryService {
    
    @Autowired
    private IncomeCategoryRepository incomeCategoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<IncomeCategoryDTO> getAllCategories() {
        return incomeCategoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeCategoryDTO> getActiveCategories() {
        return incomeCategoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public IncomeCategoryDTO getCategoryById(Integer id) {
        return incomeCategoryRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + id));
    }
    
    public IncomeCategoryDTO createCategory(IncomeCategoryRequest request) {
        if (incomeCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        
        IncomeCategory category = new IncomeCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        category.setCreatedBy(getCurrentUser());
        
        IncomeCategory savedCategory = incomeCategoryRepository.save(category);
        return mapToDTO(savedCategory);
    }
    
    public IncomeCategoryDTO updateCategory(Integer id, IncomeCategoryRequest request) {
        IncomeCategory category = incomeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + id));
        
        // Check if name is changed and if new name already exists
        if (!category.getName().equals(request.getName()) && 
                incomeCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        
        IncomeCategory updatedCategory = incomeCategoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }
    
    public void deleteCategory(Integer id) {
        IncomeCategory category = incomeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + id));
        
        // Soft delete - just mark as inactive
        category.setIsActive(false);
        incomeCategoryRepository.save(category);
    }
    
    private IncomeCategoryDTO mapToDTO(IncomeCategory category) {
        IncomeCategoryDTO dto = new IncomeCategoryDTO();
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
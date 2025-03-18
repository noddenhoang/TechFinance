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

import java.time.LocalDate;
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
        
        // Only update name if provided and different from current value
        if (request.getName() != null && !request.getName().isEmpty()) {
            // Check if name is changed and if new name already exists
            if (!category.getName().equals(request.getName()) && 
                    incomeCategoryRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("A category with this name already exists");
            }
            category.setName(request.getName());
        }
        
        // Only update description if provided
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        
        // Only update active status if provided
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        
        IncomeCategory updatedCategory = incomeCategoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }
    
    public void deleteCategory(Integer id) {
        if (!incomeCategoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Income category not found with id: " + id);
        }
        incomeCategoryRepository.deleteById(id);
    }
    
    private IncomeCategoryDTO mapToDTO(IncomeCategory category) {
        IncomeCategoryDTO dto = new IncomeCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
    
    /**
     * Filter income categories based on optional criteria
     * If all parameters are null, returns all categories
     * 
     * @param name Filter by name containing this string (case-insensitive)
     * @param isActive Filter by active status
     * @param createdAt Filter by creation date (date only, not time)
     * @return List of filtered income categories
     */
    public List<IncomeCategoryDTO> filterCategories(String name, Boolean isActive, LocalDate createdAt) {
        // If all filter parameters are null, return all categories
        if (name == null && isActive == null && createdAt == null) {
            return getAllCategories();
        }

        List<IncomeCategory> categories = incomeCategoryRepository.findAll();
        
        return categories.stream()
                .filter(category -> {
                    // Filter by name if provided - using simple LIKE pattern
                    if (name != null && !name.isEmpty()) {
                        if (category.getName() == null) {
                            return false;
                        }
                        // Simple case-insensitive LIKE operation
                        if (!category.getName().toLowerCase().contains(name.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by isActive if provided
                    if (isActive != null && 
                        (category.getIsActive() == null || 
                        !category.getIsActive().equals(isActive))) {
                        return false;
                    }
                    
                    // Filter by createdAt date if provided
                    if (createdAt != null && 
                        (category.getCreatedAt() == null || 
                        !LocalDate.from(category.getCreatedAt()).equals(createdAt))) {
                        return false;
                    }
                    
                    return true;
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
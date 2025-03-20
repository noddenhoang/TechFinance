package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.IncomeBudgetDTO;
import com.techzenacademy.TechFinance.dto.IncomeBudgetRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.IncomeBudget;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeCategoryRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IncomeBudgetService {
    
    @Autowired
    private IncomeBudgetRepository budgetRepository;
    
    @Autowired
    private IncomeCategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<IncomeBudgetDTO> getAllBudgets() {
        return budgetRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public PageResponse<IncomeBudgetDTO> getPagedBudgets(Integer year, Integer month, Integer categoryId, Pageable pageable) {
        Page<IncomeBudget> budgetPage;
        
        if (year != null && month != null && categoryId != null) {
            // Mã xử lý đặc biệt cho trường hợp cả 3 tham số đều có
            Optional<IncomeBudget> budget = budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month);
            if (budget.isPresent()) {
                List<IncomeBudget> budgetList = new ArrayList<>();
                budgetList.add(budget.get());
                budgetPage = new PageImpl<>(budgetList, pageable, 1);
            } else {
                budgetPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
        } else if (year != null && month != null) {
            budgetPage = budgetRepository.findByYearAndMonth(year, month, pageable);
        } else if (year != null) {
            budgetPage = budgetRepository.findByYear(year, pageable);
        } else if (categoryId != null) {
            budgetPage = budgetRepository.findByCategoryId(categoryId, pageable);
        } else {
            budgetPage = budgetRepository.findAll(pageable);
        }
        
        // Chuyển đổi Page<IncomeBudget> sang Page<IncomeBudgetDTO>
        Page<IncomeBudgetDTO> dtoPage = budgetPage.map(this::mapToDTO);
        
        // Tạo PageResponse từ Page<IncomeBudgetDTO> - đồng bộ với cách làm trong CustomerService và các service khác
        return new PageResponse<>(dtoPage);
    }
    
    public List<IncomeBudgetDTO> getBudgetsByYearAndMonth(Integer year, Integer month) {
        return budgetRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeBudgetDTO> getBudgetsByYear(Integer year) {
        return budgetRepository.findByYear(year).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeBudgetDTO> getBudgetsByCategory(Integer categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + categoryId));
                
        return budgetRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public IncomeBudgetDTO getBudgetById(Integer id) {
        return budgetRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Income budget not found with id: " + id));
    }
    
    public IncomeBudgetDTO getBudgetByCategoryAndPeriod(Integer categoryId, Integer year, Integer month) {
        return budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Income budget not found for category id: %d, year: %d, month: %d", 
                                  categoryId, year, month)));
    }
    
    @Transactional
    public IncomeBudgetDTO createBudget(IncomeBudgetRequest request) {
        // Check if budget already exists for this category, year and month
        if (budgetRepository.existsByCategoryIdAndYearAndMonth(
                request.getCategoryId(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException(
                String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                              request.getCategoryId(), request.getYear(), request.getMonth()));
        }
        
        IncomeBudget budget = new IncomeBudget();
        updateBudgetFromRequest(budget, request);
        budget.setCreatedBy(getCurrentUser());
        
        IncomeBudget savedBudget = budgetRepository.save(budget);
        return mapToDTO(savedBudget);
    }
    
    @Transactional
    public IncomeBudgetDTO updateBudget(Integer id, IncomeBudgetRequest request) {
        IncomeBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Income budget not found with id: " + id));
        
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
            
            Optional<IncomeBudget> existingBudget = budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month);
            if (existingBudget.isPresent() && !existingBudget.get().getId().equals(id)) {
                throw new IllegalArgumentException(
                    String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                                  categoryId, year, month));
            }
        }
        
        // Update only provided fields
        if (request.getCategoryId() != null) {
            IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
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
        
        IncomeBudget updatedBudget = budgetRepository.save(budget);
        return mapToDTO(updatedBudget);
    }
    
    @Transactional
    public void deleteBudget(Integer id) {
        if (!budgetRepository.existsById(id)) {
            throw new EntityNotFoundException("Income budget not found with id: " + id);
        }
        budgetRepository.deleteById(id);
    }
    
    private void updateBudgetFromRequest(IncomeBudget budget, IncomeBudgetRequest request) {
        IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
        budget.setCategory(category);
        
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());
        budget.setAmount(request.getAmount());
        budget.setNotes(request.getNotes());
    }
    
    private IncomeBudgetDTO mapToDTO(IncomeBudget budget) {
        IncomeBudgetDTO dto = new IncomeBudgetDTO();
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
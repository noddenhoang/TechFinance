package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ExpenseCategoryDTO;
import com.techzenacademy.TechFinance.dto.ExpenseCategoryRequest;
import com.techzenacademy.TechFinance.service.impl.ExpenseCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.techzenacademy.TechFinance.dto.page.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {
    
    @Autowired
    private ExpenseCategoryService expenseCategoryService;
    
    @GetMapping
    public ResponseEntity<PageResponse<ExpenseCategoryDTO>> getCategories(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") String[] sort) {
        
        // Tạo đối tượng Sort
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortField);
        
        // Tạo đối tượng Pageable với kích thước mặc định là 8
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        // Lấy dữ liệu đã phân trang
        PageResponse<ExpenseCategoryDTO> pagedResponse = 
                expenseCategoryService.getPagedCategories(name, isActive, pageable);
                
        return ResponseEntity.ok(pagedResponse);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseCategoryDTO> getCategoryById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(expenseCategoryService.getCategoryById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ExpenseCategoryDTO> createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        return new ResponseEntity<>(expenseCategoryService.createCategory(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ExpenseCategoryDTO> updateCategory(
            @PathVariable("id") Integer id, 
            @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(expenseCategoryService.updateCategory(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Integer id) {
        expenseCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
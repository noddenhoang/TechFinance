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

import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {
    
    @Autowired
    private ExpenseCategoryService expenseCategoryService;
    
    @GetMapping
    public ResponseEntity<List<ExpenseCategoryDTO>> getCategories(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(expenseCategoryService.filterCategories(name, isActive));
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
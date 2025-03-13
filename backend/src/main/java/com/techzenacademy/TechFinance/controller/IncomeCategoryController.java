package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.IncomeCategoryDTO;
import com.techzenacademy.TechFinance.dto.IncomeCategoryRequest;
import com.techzenacademy.TechFinance.service.impl.IncomeCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/income-categories")
public class IncomeCategoryController {
    
    @Autowired
    private IncomeCategoryService incomeCategoryService;
    
    @GetMapping
    public ResponseEntity<List<IncomeCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(incomeCategoryService.getAllCategories());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<IncomeCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(incomeCategoryService.getActiveCategories());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<IncomeCategoryDTO> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(incomeCategoryService.getCategoryById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<IncomeCategoryDTO> createCategory(@Valid @RequestBody IncomeCategoryRequest request) {
        return new ResponseEntity<>(incomeCategoryService.createCategory(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<IncomeCategoryDTO> updateCategory(
            @PathVariable Integer id, 
            @Valid @RequestBody IncomeCategoryRequest request) {
        return ResponseEntity.ok(incomeCategoryService.updateCategory(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        incomeCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
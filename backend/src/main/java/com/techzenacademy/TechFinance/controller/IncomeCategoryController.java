package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.IncomeCategoryDTO;
import com.techzenacademy.TechFinance.dto.IncomeCategoryRequest;
import com.techzenacademy.TechFinance.service.impl.IncomeCategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/income-categories")
public class IncomeCategoryController {
    
    @Autowired
    private IncomeCategoryService incomeCategoryService;
    
    @GetMapping
    public ResponseEntity<List<IncomeCategoryDTO>> getCategories(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "createdAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt) {
        return ResponseEntity.ok(incomeCategoryService.filterCategories(name, isActive, createdAt));
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<IncomeCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(incomeCategoryService.getActiveCategories());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<IncomeCategoryDTO> getCategoryById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(incomeCategoryService.getCategoryById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<IncomeCategoryDTO> createCategory(@Valid @RequestBody IncomeCategoryRequest request) {
        return new ResponseEntity<>(incomeCategoryService.createCategory(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<IncomeCategoryDTO> updateCategory(
            @PathVariable("id") Integer id, 
            @RequestBody IncomeCategoryRequest request) {  // Removed @Valid annotation
        return ResponseEntity.ok(incomeCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Integer id) {
        incomeCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
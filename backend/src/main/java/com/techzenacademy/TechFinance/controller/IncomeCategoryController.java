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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.techzenacademy.TechFinance.dto.page.PageResponse;

@RestController
@RequestMapping("/api/income-categories")
public class IncomeCategoryController {
    
    @Autowired
    private IncomeCategoryService incomeCategoryService;
    
    @GetMapping
    public ResponseEntity<PageResponse<IncomeCategoryDTO>> getCategories(
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
        PageResponse<IncomeCategoryDTO> pagedResponse = 
                incomeCategoryService.getPagedCategories(name, isActive, pageable);
                
        return ResponseEntity.ok(pagedResponse);
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
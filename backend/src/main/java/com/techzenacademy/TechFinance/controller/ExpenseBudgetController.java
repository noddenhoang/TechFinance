package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ExpenseBudgetDTO;
import com.techzenacademy.TechFinance.dto.ExpenseBudgetRequest;
import com.techzenacademy.TechFinance.service.impl.ExpenseBudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-budgets")
public class ExpenseBudgetController {
    
    @Autowired
    private ExpenseBudgetService expenseBudgetService;
    
    @GetMapping
    public ResponseEntity<List<ExpenseBudgetDTO>> getAllBudgets() {
        return ResponseEntity.ok(expenseBudgetService.getAllBudgets());
    }
    
    @GetMapping("/year/{year}")
    public ResponseEntity<List<ExpenseBudgetDTO>> getBudgetsByYear(@PathVariable("year") Integer year) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetsByYear(year));
    }
    
    @GetMapping("/period")
    public ResponseEntity<List<ExpenseBudgetDTO>> getBudgetsByYearAndMonth(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetsByYearAndMonth(year, month));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExpenseBudgetDTO>> getBudgetsByCategory(@PathVariable("categoryId") Integer categoryId) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetsByCategory(categoryId));
    }
    
    @GetMapping("/category/{categoryId}/period")
    public ResponseEntity<ExpenseBudgetDTO> getBudgetByCategoryAndPeriod(
            @PathVariable("categoryId") Integer categoryId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetByCategoryAndPeriod(categoryId, year, month));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseBudgetDTO> getBudgetById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ExpenseBudgetDTO> createBudget(@Valid @RequestBody ExpenseBudgetRequest request) {
        return new ResponseEntity<>(expenseBudgetService.createBudget(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ExpenseBudgetDTO> updateBudget(
            @PathVariable("id") Integer id,
            @RequestBody ExpenseBudgetRequest request) {  // Removed @Valid annotation
        return ResponseEntity.ok(expenseBudgetService.updateBudget(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deleteBudget(@PathVariable("id") Integer id) {
        expenseBudgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
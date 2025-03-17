package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ExpenseTransactionDTO;
import com.techzenacademy.TechFinance.dto.ExpenseTransactionRequest;
import com.techzenacademy.TechFinance.service.impl.ExpenseTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expense-transactions")
public class ExpenseTransactionController {
    
    @Autowired
    private ExpenseTransactionService expenseTransactionService;
    
    @GetMapping
    public ResponseEntity<List<ExpenseTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(expenseTransactionService.getAllTransactions());
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByDateRange(startDate, endDate));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByCategory(@PathVariable("categoryId") Integer categoryId) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByCategory(categoryId));
    }
    
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsBySupplier(@PathVariable("supplierId") Integer supplierId) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsBySupplier(supplierId));
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByMonth(year, month));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseTransactionDTO> getTransactionById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionById(id));
    }
    
    @PostMapping
    public ResponseEntity<ExpenseTransactionDTO> createTransaction(@Valid @RequestBody ExpenseTransactionRequest request) {
        return new ResponseEntity<>(expenseTransactionService.createTransaction(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseTransactionDTO> updateTransaction(
            @PathVariable("id") Integer id,
            @RequestBody ExpenseTransactionRequest request) {  // Removed @Valid annotation
        return ResponseEntity.ok(expenseTransactionService.updateTransaction(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Integer id) {
        expenseTransactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
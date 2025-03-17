package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.IncomeTransactionDTO;
import com.techzenacademy.TechFinance.dto.IncomeTransactionRequest;
import com.techzenacademy.TechFinance.service.impl.IncomeTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/income-transactions")
public class IncomeTransactionController {
    
    @Autowired
    private IncomeTransactionService incomeTransactionService;
    
    @GetMapping
    public ResponseEntity<List<IncomeTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(incomeTransactionService.getAllTransactions());
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<IncomeTransactionDTO>> getTransactionsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionsByDateRange(startDate, endDate));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<IncomeTransactionDTO>> getTransactionsByCategory(@PathVariable("categoryId") Integer categoryId) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionsByCategory(categoryId));
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<IncomeTransactionDTO>> getTransactionsByCustomer(@PathVariable("customerId") Integer customerId) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionsByCustomer(customerId));
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<IncomeTransactionDTO>> getTransactionsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionsByMonth(year, month));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<IncomeTransactionDTO> getTransactionById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionById(id));
    }
    
    @PostMapping
    public ResponseEntity<IncomeTransactionDTO> createTransaction(@Valid @RequestBody IncomeTransactionRequest request) {
        return new ResponseEntity<>(incomeTransactionService.createTransaction(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<IncomeTransactionDTO> updateTransaction(
            @PathVariable("id") Integer id,
            @RequestBody IncomeTransactionRequest request) {  // Removed @Valid annotation
        return ResponseEntity.ok(incomeTransactionService.updateTransaction(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Integer id) {
        incomeTransactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
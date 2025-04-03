package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ExpenseBudgetDTO;
import com.techzenacademy.TechFinance.dto.ExpenseBudgetRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.service.impl.ExpenseBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-budgets")
@Tag(name = "Expense Budgets", description = "API quản lý ngân sách chi tiêu")
public class ExpenseBudgetController {
    
    @Autowired
    private ExpenseBudgetService expenseBudgetService;
    
    @GetMapping
    @Operation(summary = "Lấy danh sách ngân sách chi tiêu có phân trang")
    public ResponseEntity<PageResponse<ExpenseBudgetDTO>> getBudgets(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") String[] sort) {
        
        // Parse sort parameters
        String sortField = sort[0];
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (sort.length > 1 && sort[1].equalsIgnoreCase("desc")) {
            sortDirection = Sort.Direction.DESC;
        }
        
        Sort sortObj = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        PageResponse<ExpenseBudgetDTO> result = expenseBudgetService.getPagedBudgets(year, month, categoryId, pageable);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Lấy ngân sách theo ID")
    public ResponseEntity<ExpenseBudgetDTO> getBudgetById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(expenseBudgetService.getBudgetById(id));
    }
    
    @GetMapping("/refresh")
    @Operation(summary = "Cập nhật lại ngân sách chi tiêu dựa trên chi tiêu thực tế")
    public ResponseEntity<Void> refreshBudgets(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        // Nếu không cung cấp năm và tháng, hệ thống sẽ cập nhật cho tháng hiện tại
        expenseBudgetService.refreshBudgets(year, month);
        return ResponseEntity.ok().build();
    }
}
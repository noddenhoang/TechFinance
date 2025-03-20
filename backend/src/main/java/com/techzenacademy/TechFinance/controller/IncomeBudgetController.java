package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.IncomeBudgetDTO;
import com.techzenacademy.TechFinance.dto.IncomeBudgetRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.service.impl.IncomeBudgetService;
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

import java.util.List;

@RestController
@RequestMapping("/api/income-budgets")
@Tag(name = "Income Budgets", description = "API quản lý ngân sách thu nhập")
public class IncomeBudgetController {
    
    @Autowired
    private IncomeBudgetService incomeBudgetService;
    
    @GetMapping
    @Operation(summary = "Lấy danh sách ngân sách thu nhập có phân trang")
    public ResponseEntity<PageResponse<IncomeBudgetDTO>> getBudgets(
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
        
        PageResponse<IncomeBudgetDTO> result = incomeBudgetService.getPagedBudgets(year, month, categoryId, pageable);
        return ResponseEntity.ok(result);
    }
    
    // @GetMapping("/all")
    // @Operation(summary = "Lấy tất cả ngân sách thu nhập không phân trang")
    // public ResponseEntity<List<IncomeBudgetDTO>> getAllBudgets() {
    //     return ResponseEntity.ok(incomeBudgetService.getAllBudgets());
    // }
    
    // @GetMapping("/year/{year}")
    // @Operation(summary = "Lấy ngân sách theo năm")
    // public ResponseEntity<List<IncomeBudgetDTO>> getBudgetsByYear(@PathVariable("year") Integer year) {
    //     return ResponseEntity.ok(incomeBudgetService.getBudgetsByYear(year));
    // }
    
    // @GetMapping("/period")
    // @Operation(summary = "Lấy ngân sách theo năm và tháng")
    // public ResponseEntity<List<IncomeBudgetDTO>> getBudgetsByYearAndMonth(
    //         @RequestParam("year") Integer year,
    //         @RequestParam("month") Integer month) {
    //     return ResponseEntity.ok(incomeBudgetService.getBudgetsByYearAndMonth(year, month));
    // }
    
    // @GetMapping("/category/{categoryId}")
    // @Operation(summary = "Lấy ngân sách theo danh mục")
    // public ResponseEntity<List<IncomeBudgetDTO>> getBudgetsByCategory(@PathVariable("categoryId") Integer categoryId) {
    //     return ResponseEntity.ok(incomeBudgetService.getBudgetsByCategory(categoryId));
    // }
    
    // @GetMapping("/category/{categoryId}/period")
    // @Operation(summary = "Lấy ngân sách theo danh mục, năm và tháng")
    // public ResponseEntity<IncomeBudgetDTO> getBudgetByCategoryAndPeriod(
    //         @PathVariable("categoryId") Integer categoryId,
    //         @RequestParam("year") Integer year,
    //         @RequestParam("month") Integer month) {
    //     return ResponseEntity.ok(incomeBudgetService.getBudgetByCategoryAndPeriod(categoryId, year, month));
    // }
    
    @GetMapping("/{id}")
    @Operation(summary = "Lấy ngân sách theo ID")
    public ResponseEntity<IncomeBudgetDTO> getBudgetById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(incomeBudgetService.getBudgetById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Tạo mới ngân sách thu nhập")
    public ResponseEntity<IncomeBudgetDTO> createBudget(@Valid @RequestBody IncomeBudgetRequest request) {
        return new ResponseEntity<>(incomeBudgetService.createBudget(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Cập nhật ngân sách thu nhập")
    public ResponseEntity<IncomeBudgetDTO> updateBudget(
            @PathVariable("id") Integer id,
            @RequestBody IncomeBudgetRequest request) {
        return ResponseEntity.ok(incomeBudgetService.updateBudget(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Xóa ngân sách thu nhập")
    public ResponseEntity<Void> deleteBudget(@PathVariable("id") Integer id) {
        incomeBudgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
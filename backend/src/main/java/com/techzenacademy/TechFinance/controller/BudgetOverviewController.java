package com.techzenacademy.TechFinance.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.service.impl.BudgetOverviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/budget-overview")
@Tag(name = "Budget Overview", description = "API tổng quan ngân sách")
public class BudgetOverviewController {
    
    @Autowired
    private BudgetOverviewService budgetOverviewService;
    
    @GetMapping
    @Operation(summary = "Lấy tổng quan ngân sách")
    public ResponseEntity<Map<String, Object>> getBudgetOverview(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        // Nếu không cung cấp năm và tháng, hệ thống sẽ lấy dữ liệu cho năm hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        Map<String, Object> overview = budgetOverviewService.getBudgetOverview(year, month);
        return ResponseEntity.ok(overview);
    }
} 
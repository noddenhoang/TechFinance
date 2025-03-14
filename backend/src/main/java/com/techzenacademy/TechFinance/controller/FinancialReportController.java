package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.CategoryReportDTO;
import com.techzenacademy.TechFinance.dto.MonthlyFinancialReportDTO;
import com.techzenacademy.TechFinance.service.impl.FinancialReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class FinancialReportController {
    
    @Autowired
    private FinancialReportService financialReportService;
    
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyFinancialReportDTO> getMonthlyReport(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.ok(financialReportService.getMonthlyReport(year, month));
    }
    
    @GetMapping("/yearly")
    public ResponseEntity<List<MonthlyFinancialReportDTO>> getYearlyReports(
            @RequestParam("year") Integer year) {
        return ResponseEntity.ok(financialReportService.getYearlyReports(year));
    }
    
    @GetMapping("/category-breakdown")
    public ResponseEntity<CategoryReportDTO> getCategoryBreakdown(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.ok(financialReportService.getCategoryBreakdown(year, month));
    }
}
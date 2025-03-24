package com.techzenacademy.TechFinance.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.service.SupplierReportService;

@RestController
@RequestMapping("/api/reports/suppliers")
public class SupplierReportController {

    private final SupplierReportService supplierReportService;

    @Autowired
    public SupplierReportController(SupplierReportService supplierReportService) {
        this.supplierReportService = supplierReportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlySupplierReport(
            @PathVariable int year,
            @PathVariable int month) {
        
        Map<String, Object> report = supplierReportService.generateMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/quarterly/{year}/{quarter}")
    public ResponseEntity<Map<String, Object>> getQuarterlySupplierReport(
            @PathVariable int year,
            @PathVariable int quarter) {
        
        Map<String, Object> report = supplierReportService.generateQuarterlyReport(year, quarter);
        return ResponseEntity.ok(report);
    }
} 
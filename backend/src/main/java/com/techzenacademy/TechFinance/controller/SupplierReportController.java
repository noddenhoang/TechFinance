package com.techzenacademy.TechFinance.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySupplierReport(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        // Nếu không cung cấp năm và tháng, sử dụng tháng hiện tại
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = year != null ? year : now.getYear();
            month = month != null ? month : now.getMonthValue();
        }
        
        Map<String, Object> report = supplierReportService.generateMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/quarterly")
    public ResponseEntity<Map<String, Object>> getQuarterlySupplierReport(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "quarter", required = false) Integer quarter) {
        
        // Nếu không cung cấp năm và quý, sử dụng quý hiện tại
        if (year == null || quarter == null) {
            LocalDate now = LocalDate.now();
            year = year != null ? year : now.getYear();
            quarter = quarter != null ? quarter : (now.getMonthValue() - 1) / 3 + 1;
        }
        
        // Đảm bảo quý từ 1-4
        if (quarter < 1 || quarter > 4) {
            quarter = 1;
        }
        
        Map<String, Object> report = supplierReportService.generateQuarterlyReport(year, quarter);
        return ResponseEntity.ok(report);
    }
} 
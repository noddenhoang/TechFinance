package com.techzenacademy.TechFinance.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.service.CustomerReportService;

@RestController
@RequestMapping("/api/reports/customers")
public class CustomerReportController {

    private final CustomerReportService customerReportService;

    @Autowired
    public CustomerReportController(CustomerReportService customerReportService) {
        this.customerReportService = customerReportService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyCustomerReport(
            @PathVariable int year,
            @PathVariable int month) {
        
        Map<String, Object> report = customerReportService.generateMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/quarterly/{year}/{quarter}")
    public ResponseEntity<Map<String, Object>> getQuarterlyCustomerReport(
            @PathVariable int year,
            @PathVariable int quarter) {
        
        Map<String, Object> report = customerReportService.generateQuarterlyReport(year, quarter);
        return ResponseEntity.ok(report);
    }
} 
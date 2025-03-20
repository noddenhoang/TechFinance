package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO;
import com.techzenacademy.TechFinance.service.impl.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Financial Reports", description = "API báo cáo tài chính")
public class FinancialReportController {

    @Autowired
    private FinancialReportService reportService;
    
    @GetMapping("/monthly")
    @Operation(summary = "Lấy báo cáo tháng")
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        // Nếu không cung cấp năm và tháng, sử dụng tháng hiện tại
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = year != null ? year : now.getYear();
            month = month != null ? month : now.getMonthValue();
        }
        
        return ResponseEntity.ok(reportService.generateMonthlyReport(year, month));
    }
}
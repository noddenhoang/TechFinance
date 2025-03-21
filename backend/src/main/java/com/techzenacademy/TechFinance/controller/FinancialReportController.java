package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.report.MonthlyReportDTO;
import com.techzenacademy.TechFinance.service.impl.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/quarterly")
    @Operation(summary = "Lấy báo cáo quý theo từng tháng")
    public ResponseEntity<List<MonthlyReportDTO>> getQuarterlyReport(
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
        
        return ResponseEntity.ok(reportService.generateQuarterlyReport(year, quarter));
    }

    @GetMapping("/yearly")
    @Operation(summary = "Lấy báo cáo năm theo từng tháng")
    public ResponseEntity<List<MonthlyReportDTO>> getYearlyReport(
            @RequestParam(name = "year", required = false) Integer year) {
        
        // Nếu không cung cấp năm, sử dụng năm hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        return ResponseEntity.ok(reportService.generateYearlyReport(year));
    }
}
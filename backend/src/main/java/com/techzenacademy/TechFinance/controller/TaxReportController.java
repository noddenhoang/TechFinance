package com.techzenacademy.TechFinance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.dto.TaxReportDTO;
import com.techzenacademy.TechFinance.service.impl.TaxReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reports/tax")
@Tag(name = "Tax Reports", description = "API báo cáo thuế")
public class TaxReportController {

    @Autowired
    private TaxReportService taxReportService;

    @GetMapping("/monthly")
    @Operation(summary = "Lấy báo cáo thuế theo tháng")
    public ResponseEntity<TaxReportDTO> getMonthlyTaxReport(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        
        return ResponseEntity.ok(taxReportService.getTaxReport(year, month));
    }

    @GetMapping("/yearly")
    @Operation(summary = "Lấy báo cáo thuế theo năm")
    public ResponseEntity<List<TaxReportDTO>> getYearlyTaxReports(
            @RequestParam(name = "year") Integer year) {
        
        return ResponseEntity.ok(taxReportService.getYearlyTaxReports(year));
    }

    @GetMapping("/range")
    @Operation(summary = "Lấy báo cáo thuế theo khoảng thời gian")
    public ResponseEntity<List<TaxReportDTO>> getTaxReportsByDateRange(
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(taxReportService.getTaxReportsByDateRange(startDate, endDate));
    }
}
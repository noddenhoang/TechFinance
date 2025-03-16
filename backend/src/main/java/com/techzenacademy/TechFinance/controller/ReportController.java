package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.CustomerReportDTO;
import com.techzenacademy.TechFinance.dto.SupplierReportDTO;
import com.techzenacademy.TechFinance.service.impl.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "API báo cáo tài chính")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/customers")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy danh sách báo cáo tổng quan theo khách hàng")
    public ResponseEntity<List<CustomerReportDTO>> getCustomerReports(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "sortBy", defaultValue = "totalAmount") String sortBy) {
        
        return ResponseEntity.ok(reportService.getCustomerReports(startDate, endDate, year, month, sortBy));
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy chi tiết báo cáo theo khách hàng")
    public ResponseEntity<CustomerReportDTO> getCustomerDetailReport(
            @PathVariable Integer customerId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        return ResponseEntity.ok(reportService.getCustomerDetailReport(customerId, startDate, endDate, year, month));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy danh sách báo cáo tổng quan theo nhà cung cấp")
    public ResponseEntity<List<SupplierReportDTO>> getSupplierReports(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "sortBy", defaultValue = "totalAmount") String sortBy) {
        
        return ResponseEntity.ok(reportService.getSupplierReports(startDate, endDate, year, month, sortBy));
    }

    @GetMapping("/suppliers/{supplierId}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy chi tiết báo cáo theo nhà cung cấp")
    public ResponseEntity<SupplierReportDTO> getSupplierDetailReport(
            @PathVariable Integer supplierId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        return ResponseEntity.ok(reportService.getSupplierDetailReport(supplierId, startDate, endDate, year, month));
    }
}
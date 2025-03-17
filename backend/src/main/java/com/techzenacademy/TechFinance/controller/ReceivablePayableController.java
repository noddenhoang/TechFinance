package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ReceivablePayableReportDTO;
import com.techzenacademy.TechFinance.service.impl.ReceivablePayableService;
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
@RequestMapping("/api/reports/receivable-payable")
@Tag(name = "Receivable Payable Reports", description = "API báo cáo tiền phải thu, phải trả và lỗi giao dịch")
public class ReceivablePayableController {

    @Autowired
    private ReceivablePayableService receivablePayableService;

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy báo cáo tiền phải thu, phải trả theo tháng")
    public ResponseEntity<ReceivablePayableReportDTO> getMonthlyReport(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        
        return ResponseEntity.ok(receivablePayableService.getReceivablePayableReport(year, month));
    }

    @GetMapping("/yearly")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy báo cáo tiền phải thu, phải trả theo năm")
    public ResponseEntity<List<ReceivablePayableReportDTO>> getYearlyReports(
            @RequestParam(name = "year") Integer year) {
        
        return ResponseEntity.ok(receivablePayableService.getYearlyReceivablePayableReports(year));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Lấy báo cáo tiền phải thu, phải trả theo khoảng thời gian")
    public ResponseEntity<ReceivablePayableReportDTO> getReportByDateRange(
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(receivablePayableService.getReceivablePayableReportByDateRange(startDate, endDate));
    }
}
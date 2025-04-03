package com.techzenacademy.TechFinance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.dto.ReceivablePayableDTO;
import com.techzenacademy.TechFinance.dto.ReceivablePayableReportDTO;
import com.techzenacademy.TechFinance.service.impl.ReceivablePayableService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports/receivable-payable")
@RequiredArgsConstructor
public class ReceivablePayableController {
    
    private final ReceivablePayableService receivablePayableService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceivablePayableDTO> getReceivablePayableReport(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        ReceivablePayableDTO report = receivablePayableService.getReceivablePayableChartData(year, month);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/detailed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReceivablePayableReportDTO> getDetailedReceivablePayableReport(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        
        ReceivablePayableReportDTO report = receivablePayableService.getReceivablePayableReport(year, month);
        return ResponseEntity.ok(report);
    }
}
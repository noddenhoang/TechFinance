// package com.techzenacademy.TechFinance.controller;

// import com.techzenacademy.TechFinance.dto.CategoryReportDTO;
// import com.techzenacademy.TechFinance.dto.MonthlyFinancialReportDTO;
// import com.techzenacademy.TechFinance.dto.PeriodComparisonReportDTO;
// import com.techzenacademy.TechFinance.service.impl.FinancialReportService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.format.annotation.DateTimeFormat;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDate;
// import java.util.List;

// @RestController
// @RequestMapping("/api/reports")
// public class FinancialReportController {
    
//     @Autowired
//     private FinancialReportService financialReportService;
    
//     @GetMapping("/monthly")
//     public ResponseEntity<MonthlyFinancialReportDTO> getMonthlyReport(
//             @RequestParam("year") Integer year,
//             @RequestParam("month") Integer month) {
//         return ResponseEntity.ok(financialReportService.getMonthlyReport(year, month));
//     }
    
//     @GetMapping("/yearly")
//     public ResponseEntity<List<MonthlyFinancialReportDTO>> getYearlyReports(
//             @RequestParam("year") Integer year) {
//         return ResponseEntity.ok(financialReportService.getYearlyReports(year));
//     }
    
//     @GetMapping("/category-breakdown")
//     public ResponseEntity<CategoryReportDTO> getCategoryBreakdown(
//             @RequestParam("year") Integer year,
//             @RequestParam("month") Integer month) {
//         return ResponseEntity.ok(financialReportService.getCategoryBreakdown(year, month));
//     }
    
//     @GetMapping("/category-breakdown/filtered")
//     public ResponseEntity<CategoryReportDTO> getCategoryBreakdownWithFilters(
//             @RequestParam("year") Integer year,
//             @RequestParam("month") Integer month,
//             @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
//             @RequestParam(value = "sortBy", required = false, defaultValue = "amount") String sortBy) {
//         return ResponseEntity.ok(financialReportService.getCategoryBreakdownWithFilters(year, month, activeOnly, sortBy));
//     }
    
//     @GetMapping("/category-comparison")
//     public ResponseEntity<PeriodComparisonReportDTO> compareCategoryReports(
//             @RequestParam("currentYear") Integer currentYear,
//             @RequestParam("currentMonth") Integer currentMonth,
//             @RequestParam("previousYear") Integer previousYear,
//             @RequestParam("previousMonth") Integer previousMonth) {
//         return ResponseEntity.ok(financialReportService.comparePeriods(
//             currentYear, currentMonth, previousYear, previousMonth));
//     }
// }
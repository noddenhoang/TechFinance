package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.TaxReportDTO;
import com.techzenacademy.TechFinance.entity.TaxRecord;
import com.techzenacademy.TechFinance.repository.TaxRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaxReportService {

    @Autowired
    private TaxRecordRepository taxRecordRepository;

    /**
     * Lấy báo cáo thuế theo tháng và năm
     */
    public TaxReportDTO getTaxReport(Integer year, Integer month) {
        // Kiểm tra tham số
        if (year < 2000 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Giá trị năm hoặc tháng không hợp lệ");
        }
        
        // Gọi stored procedure để tính và lưu báo cáo thuế
        List<Object[]> result = taxRecordRepository.generateTaxReport(year, month);
        
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Không thể tạo báo cáo thuế cho " + month + "/" + year);
        }
        
        // Lấy kết quả từ stored procedure
        Object[] row = result.get(0);
        TaxReportDTO report = new TaxReportDTO();
        report.setYear((Integer) row[0]);
        report.setMonth((Integer) row[1]);
        report.setIncomeTax(new BigDecimal(row[2].toString()));
        report.setExpenseTax(new BigDecimal(row[3].toString()));
        report.setTaxDifference(new BigDecimal(row[4].toString()));
        
        return report;
    }
    
    /**
     * Lấy báo cáo thuế theo năm
     */
    public List<TaxReportDTO> getYearlyTaxReports(Integer year) {
        // Kiểm tra tham số
        if (year < 2000) {
            throw new IllegalArgumentException("Giá trị năm không hợp lệ");
        }
        
        List<TaxReportDTO> reports = new ArrayList<>();
        
        // Tạo báo cáo cho tất cả các tháng trong năm
        for (int month = 1; month <= 12; month++) {
            try {
                TaxReportDTO report = getTaxReport(year, month);
                reports.add(report);
            } catch (Exception e) {
                // Nếu không có dữ liệu cho tháng, tạo báo cáo trống
                TaxReportDTO emptyReport = new TaxReportDTO();
                emptyReport.setYear(year);
                emptyReport.setMonth(month);
                emptyReport.setIncomeTax(BigDecimal.ZERO);
                emptyReport.setExpenseTax(BigDecimal.ZERO);
                emptyReport.setTaxDifference(BigDecimal.ZERO);
                reports.add(emptyReport);
            }
        }
        
        return reports;
    }
    
    /**
     * Lấy báo cáo thuế theo khoảng thời gian
     */
    public List<TaxReportDTO> getTaxReportsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<TaxReportDTO> reports = new ArrayList<>();
        
        LocalDate currentDate = startDate.withDayOfMonth(1);
        while (!currentDate.isAfter(endDate)) {
            try {
                TaxReportDTO report = getTaxReport(currentDate.getYear(), currentDate.getMonthValue());
                reports.add(report);
            } catch (Exception e) {
                // Bỏ qua nếu không có dữ liệu
            }
            
            currentDate = currentDate.plusMonths(1);
        }
        
        return reports;
    }
}
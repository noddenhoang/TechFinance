package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TaxReportDTO {
    private Integer year;
    private Integer month;
    private BigDecimal incomeTax;     // Thuế thu nhập (10% tổng thu nhập)
    private BigDecimal expenseTax;    // Thuế chi phí (5% tổng chi phí)
    private BigDecimal taxDifference; // Chênh lệch thuế
    private String notes;             // Ghi chú (tùy chọn)
}
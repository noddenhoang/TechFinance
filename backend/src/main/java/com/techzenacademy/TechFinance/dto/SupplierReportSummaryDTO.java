package com.techzenacademy.TechFinance.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class SupplierReportSummaryDTO {
    private BigDecimal totalAmount; // Tổng giá trị giao dịch
    private BigDecimal paidAmount; // Đã trả
    private BigDecimal unpaidAmount; // Chưa trả
    private List<SupplierReportDTO> entities; // Danh sách các nhà cung cấp
} 
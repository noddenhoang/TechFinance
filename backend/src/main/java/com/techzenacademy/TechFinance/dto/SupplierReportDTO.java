package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SupplierReportDTO {
    private Integer supplierId;
    private String supplierName;
    private String email;
    private String phone;
    private BigDecimal totalAmount; // Tổng giá trị giao dịch
    private BigDecimal paidAmount; // Đã trả
    private BigDecimal unpaidAmount; // Chưa trả
    private BigDecimal percentage; // % đóng góp vào tổng chi phí
    private List<TransactionSummaryDTO> transactionsByMonth; // Chi tiết theo tháng
    private List<CategorySummaryDTO> transactionsByCategory; // Chi tiết theo danh mục
}
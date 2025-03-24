package com.techzenacademy.TechFinance.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CustomerReportSummaryDTO {
    private BigDecimal totalAmount; // Tổng giá trị giao dịch
    private BigDecimal receivedAmount; // Đã nhận
    private BigDecimal pendingAmount; // Chưa nhận
    private List<CustomerReportDTO> entities; // Danh sách các khách hàng
} 
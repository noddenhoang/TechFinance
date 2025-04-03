package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerReportDTO {
    private Integer customerId;
    private String customerName;
    private String email;
    private String phone;
    private BigDecimal totalAmount; // Tổng giá trị giao dịch
    private BigDecimal receivedAmount; // Đã nhận
    private BigDecimal pendingAmount; // Chưa nhận
    private BigDecimal percentage; // % đóng góp vào tổng thu nhập
    private List<TransactionSummaryDTO> transactionsByMonth; // Chi tiết theo tháng
    private List<CategorySummaryDTO> transactionsByCategory; // Chi tiết theo danh mục
}
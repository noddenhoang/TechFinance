package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReceivablePayableReportDTO {
    private Integer year;
    private Integer month;
    
    // Thông tin tổng hợp
    private BigDecimal totalReceivables;        // Tổng tiền phải thu
    private BigDecimal totalPayables;           // Tổng tiền phải trả
    private BigDecimal netWorkingCapitalImpact; // Ảnh hưởng vốn lưu động (Thu - Chi)
    private BigDecimal actualProfit;            // Lợi nhuận thực tế (Thu đã nhận - Chi đã trả)
    private BigDecimal potentialProfit;         // Lợi nhuận tiềm năng (bao gồm giao dịch chưa thanh toán)
    
    // Chi tiết
    private List<ReceivableDetailDTO> topReceivables;   // Top khoản phải thu
    private List<PayableDetailDTO> topPayables;         // Top khoản phải trả
    private List<AgedReceivableDTO> agedReceivables;    // Phân tích tuổi nợ phải thu
    private List<AgedPayableDTO> agedPayables;          // Phân tích tuổi nợ phải trả
    
    // Lỗi giao dịch
    private List<TransactionErrorDTO> transactionErrors;  // Các lỗi hoặc bất thường
}
package com.techzenacademy.TechFinance.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivablePayableDTO {
    // Summary fields
    private BigDecimal totalReceived;
    private BigDecimal totalPending;
    private BigDecimal totalReceivable;
    private BigDecimal totalPaid;
    private BigDecimal totalUnpaid;
    private BigDecimal totalPayable;
    
    // Monthly data for charts
    private List<MonthlyDataDTO> receivablesByMonth;
    private List<MonthlyDataDTO> payablesByMonth;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataDTO {
        private Integer month; // 1-12
        private BigDecimal amount;
    }
} 
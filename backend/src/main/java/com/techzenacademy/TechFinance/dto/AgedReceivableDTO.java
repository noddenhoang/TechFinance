package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgedReceivableDTO {
    private String agingBucket; // Ví dụ: "0-30 ngày", "31-60 ngày", "61-90 ngày", "90+ ngày"
    private BigDecimal amount;
    private BigDecimal percentage; // % của tổng tiền phải thu
}
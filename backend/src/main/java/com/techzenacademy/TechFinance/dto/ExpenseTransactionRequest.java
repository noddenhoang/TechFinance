package com.techzenacademy.TechFinance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction.PaymentStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseTransactionRequest {
    @NotNull(message = "Category ID is required")
    private Integer categoryId;
    
    @JsonProperty(value = "supplierId")
    private Integer supplierId;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean supplierIdFieldIncluded;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    private PaymentStatus paymentStatus;
    private String description;
    private String referenceNo;
    
    // Custom setter to detect if supplierId field was explicitly included in JSON
    @JsonProperty("supplierId")
    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
        this.supplierIdFieldIncluded = true;
    }
    
    public boolean hasSupplierIdField() {
        return supplierIdFieldIncluded;
    }
}
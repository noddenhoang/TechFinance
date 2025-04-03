package com.techzenacademy.TechFinance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techzenacademy.TechFinance.entity.IncomeTransaction.PaymentStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeTransactionRequest {
    @NotNull(message = "Category ID is required")
    private Integer categoryId;
    
    @JsonProperty(value = "customerId")
    private Integer customerId;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean customerIdFieldIncluded;
    
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    private PaymentStatus paymentStatus;
    private String description;
    private String referenceNo;
    
    // Custom setter to detect if customerId field was explicitly included in JSON
    @JsonProperty("customerId")
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
        this.customerIdFieldIncluded = true;
    }
    
    public boolean hasCustomerIdField() {
        return customerIdFieldIncluded;
    }
}
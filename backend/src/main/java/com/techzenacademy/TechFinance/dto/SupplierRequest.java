package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

@Data
public class SupplierRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String address;
    private String taxCode;
    private String notes;
    private Boolean isActive = true;
}
package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Data
public class CustomerRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Identification must be exactly 12 digits or empty")
    private String identification;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    private String taxCode;
    
    private String notes;
    
    private Boolean isActive = true;
}
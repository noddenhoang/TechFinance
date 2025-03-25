package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    private String password;
    
    private String fullName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    
    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Identification must be exactly 12 digits or empty")
    private String identification;
    
    private UserRole role;
    
    private Boolean isActive;
    
    // Fix for the missing setter issue - explicitly define field with correct naming
    private boolean isNewUser;
    
    // Explicitly define the setter to ensure it's available
    public void setNewUser(boolean isNewUser) {
        this.isNewUser = isNewUser;
    }
    
    public boolean isNewUser() {
        return this.isNewUser;
    }
}

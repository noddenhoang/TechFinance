package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.UserRole;
import lombok.Data;

@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String identification;
    private UserRole role;
    private Boolean isActive;
}

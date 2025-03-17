package com.techzenacademy.TechFinance.dto;

import com.techzenacademy.TechFinance.entity.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private UserDto user;
    
    // Constructor for token-only response
    public AuthResponse(String token) {
        this.token = token;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private Integer id;
        private String username;
        private String fullName;
        private String email;
        private UserRole role;
    }
}
package com.techzenacademy.TechFinance.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('admin', 'user')")
    private UserRole role;
    
    @Column(name = "full_name")
    private String fullName;
    
    private String email;
    
    private String phone;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}
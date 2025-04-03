package com.techzenacademy.TechFinance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.entity.UserRole;
import com.techzenacademy.TechFinance.repository.UserRepository;

@RestController
@RequestMapping("/api/public/init")
public class DataInitializationController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdminUser() {
        if (userRepository.findByUsername("admin").isPresent()) {
            return ResponseEntity.ok("Admin user already exists");
        }
        
        // Tạo tài khoản admin mặc định
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setFullName("Quản trị viên");
        adminUser.setEmail("admin@techfinance.com");
        adminUser.setRole(UserRole.admin);
        adminUser.setIsActive(true);
        
        userRepository.save(adminUser);
        
        // Tạo tài khoản user thông thường
        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setFullName("Người dùng");
        regularUser.setEmail("user@techfinance.com");
        regularUser.setRole(UserRole.user);
        regularUser.setIsActive(true);
        
        userRepository.save(regularUser);
        
        return ResponseEntity.ok("Admin and regular user created successfully");
    }
} 
package com.techzenacademy.TechFinance.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.dto.AuthRequest;
import com.techzenacademy.TechFinance.dto.AuthResponse;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        authService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }
        
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser(@RequestHeader("Authorization") String token) {
        AuthResponse.UserDto user = authService.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "online");
        status.put("version", "1.0.0");
        status.put("message", "API is working correctly");
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/help")
    public ResponseEntity<Map<String, String>> getLoginHelp() {
        Map<String, String> help = new HashMap<>();
        help.put("admin", "username: admin, password: admin123");
        help.put("user", "username: user, password: user123");
        help.put("note", "Use the initialization endpoint to create these accounts: POST /api/public/init/create-admin");
        
        return ResponseEntity.ok(help);
    }
    
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }
    
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleAllOptions() {
        return ResponseEntity.ok().build();
    }
}
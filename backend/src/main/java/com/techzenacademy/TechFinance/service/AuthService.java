package com.techzenacademy.TechFinance.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.dto.AuthResponse;
import com.techzenacademy.TechFinance.dto.AuthResponse.UserDto;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    public AuthResponse login(String username, String password) {
        try {
            // Authenticate user with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            if (authentication.isAuthenticated()) {
                // Generate tokens
                String token = jwtService.generateToken(username);
                String refreshToken = jwtService.generateRefreshToken(username);
                
                // Get user details
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found after authentication"));
                
                // Create UserDto
                UserDto userDto = new UserDto(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName() != null ? user.getFullName() : "",
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getRole()
                );
                
                // Return response with tokens and user details
                return new AuthResponse(token, refreshToken, userDto);
            } else {
                throw new BadCredentialsException("Authentication failed");
            }
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
    
    public void register(User user) {
        // Validate input
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Encrypt password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Ensure user is active by default
        user.setIsActive(true);
        
        userRepository.save(user);
    }

    /**
     * Log out a user by invalidating their token
     * @param token The JWT token to invalidate
     */
    public void logout(String token) {
        if (token == null) {
            return; // Nothing to do if token is null
        }
        
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            // Extract expiration date from token to use for blacklist cleanup
            Date expiryDate = jwtService.extractExpiration(token);
            tokenBlacklistService.blacklistToken(token, expiryDate);
        } catch (Exception e) {
            // Just log the error but don't throw - we want logout to always succeed
            System.err.println("Error during logout: " + e.getMessage());
        }
    }
    
    /**
     * Get current user details from token
     * @param token The JWT token
     * @return UserDto with user details
     */
    public UserDto getCurrentUser(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            String username = jwtService.extractUsername(token);
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return new UserDto(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName() != null ? user.getFullName() : "",
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getRole()
                );
            }
            
            throw new RuntimeException("User not found");
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving user details: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh an access token using a valid refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }
        
        try {
            // Validate refresh token
            if (!jwtService.validateRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            // Extract username
            String username = jwtService.extractUsername(refreshToken);
            
            // Get user from database
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Generate new access token
            String newToken = jwtService.generateToken(username);
            
            // Create UserDto
            UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName() != null ? user.getFullName() : "",
                user.getEmail() != null ? user.getEmail() : "",
                user.getRole()
            );
            
            // Return new access token with the same refresh token
            return new AuthResponse(newToken, refreshToken, userDto);
        } catch (Exception e) {
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }
}
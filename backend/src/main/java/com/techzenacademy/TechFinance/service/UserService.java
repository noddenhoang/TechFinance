package com.techzenacademy.TechFinance.service;

import com.techzenacademy.TechFinance.dto.UserDTO;
import com.techzenacademy.TechFinance.dto.UserRequest;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public UserDTO getUserById(Integer id) {
        return userRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
    
    @Transactional
    public UserDTO createUser(UserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check if identification is provided and already exists
        if (request.getIdentification() != null && !request.getIdentification().isEmpty() &&
                userRepository.existsByIdentification(request.getIdentification())) {
            throw new IllegalArgumentException("A user with this identification already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIdentification(request.getIdentification());
        user.setRole(request.getRole());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
    }
    
    @Transactional
    public UserDTO updateUser(Integer id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        
        // Check if username is changed and if new username already exists
        if (request.getUsername() != null && 
                !request.getUsername().equals(user.getUsername()) &&
                userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Check if identification is changed and if new identification already exists
        if (request.getIdentification() != null && 
                !request.getIdentification().equals(user.getIdentification()) &&
                userRepository.existsByIdentification(request.getIdentification())) {
            throw new IllegalArgumentException("A user with this identification already exists");
        }
        
        // Update fields only if they are provided
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getIdentification() != null) {
            user.setIdentification(request.getIdentification());
        }
        
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    
    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setIdentification(user.getIdentification());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}

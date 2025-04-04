package com.techzenacademy.TechFinance.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.techzenacademy.TechFinance.dto.SupplierDTO;
import com.techzenacademy.TechFinance.dto.SupplierRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.SupplierRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SupplierService {
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<SupplierDTO> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Filter suppliers with pagination
     * 
     * @param name Filter by name containing this string (case-insensitive)
     * @param email Filter by email containing this string (case-insensitive)
     * @param phone Filter by phone containing this string (case-insensitive)
     * @param address Filter by address containing this string (case-insensitive)
     * @param taxCode Filter by tax code containing this string (case-insensitive)
     * @param isActive Filter by active status
     * @param pageable Pagination information
     * @return PageResponse containing filtered suppliers
     */
    public PageResponse<SupplierDTO> getPagedSuppliers(
            String name, 
            String email, 
            String phone, 
            String address, 
            String taxCode,
            Boolean isActive, 
            Pageable pageable) {
        
        Page<Supplier> supplierPage = supplierRepository.findWithFiltersPageable(
                name, email, phone, address, taxCode, isActive, pageable);
        
        Page<SupplierDTO> dtoPage = supplierPage.map(this::mapToDTO);
        return new PageResponse<>(dtoPage);
    }
    
    public SupplierDTO getSupplierById(Integer id) {
        return supplierRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));
    }
    
    public SupplierDTO createSupplier(SupplierRequest request) {
        // Validate required fields
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }
        
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        
        if (request.getEmail() != null && supplierRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A supplier with this email already exists");
        }
        
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        supplier.setNotes(request.getNotes());
        supplier.setIsActive(request.getIsActive());
        supplier.setCreatedBy(getCurrentUser());
        
        Supplier savedSupplier = supplierRepository.save(supplier);
        return mapToDTO(savedSupplier);
    }
    
    public SupplierDTO updateSupplier(Integer id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));
        
        // Check if email is changed and if new email already exists
        if (request.getEmail() != null && !request.getEmail().equals(supplier.getEmail()) && 
                supplierRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A supplier with this email already exists");
        }
        
        // Update fields if provided
        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        
        if (request.getTaxCode() != null) {
            supplier.setTaxCode(request.getTaxCode());
        }
        
        if (request.getNotes() != null) {
            supplier.setNotes(request.getNotes());
        }
        
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }
        
        // Validate required fields after updates
        if (supplier.getEmail() == null || supplier.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (supplier.getPhone() == null || supplier.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }
        
        if (supplier.getAddress() == null || supplier.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return mapToDTO(updatedSupplier);
    }
    
    public void deleteSupplier(Integer id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Supplier not found with id: " + id);
        }
        supplierRepository.deleteById(id);
    }
    
    private SupplierDTO mapToDTO(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setTaxCode(supplier.getTaxCode());
        dto.setNotes(supplier.getNotes());
        dto.setIsActive(supplier.getIsActive());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.SupplierDTO;
import com.techzenacademy.TechFinance.dto.SupplierRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.Supplier;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.SupplierRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
     * @param pageable Pagination information
     * @return PageResponse containing filtered suppliers
     */
    public PageResponse<SupplierDTO> getPagedSuppliers(String name, Pageable pageable) {
        Page<Supplier> supplierPage;
        
        if (name != null && !name.trim().isEmpty()) {
            // Sử dụng repository để lọc theo tên
            supplierPage = supplierRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            // Nếu không có filter, lấy tất cả với phân trang
            supplierPage = supplierRepository.findAll(pageable);
        }
        
        // Map kết quả sang DTO
        Page<SupplierDTO> dtoPage = supplierPage.map(this::mapToDTO);
        
        // Trả về custom page response
        return new PageResponse<>(dtoPage);
    }
    
    public SupplierDTO getSupplierById(Integer id) {
        return supplierRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));
    }
    
    public SupplierDTO createSupplier(SupplierRequest request) {
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
        
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        supplier.setNotes(request.getNotes());
        supplier.setIsActive(request.getIsActive());
        
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
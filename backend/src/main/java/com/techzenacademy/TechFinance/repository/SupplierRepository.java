package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    List<Supplier> findByIsActiveTrue();
    boolean existsByEmail(String email);
    Optional<Supplier> findByEmail(String email);
    
    // Tìm kiếm theo tên
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Tìm kiếm theo isActive
    Page<Supplier> findByIsActive(Boolean isActive, Pageable pageable);
    
    // Tìm kiếm kết hợp theo tên và isActive
    Page<Supplier> findByNameContainingIgnoreCaseAndIsActive(String name, Boolean isActive, Pageable pageable);
}
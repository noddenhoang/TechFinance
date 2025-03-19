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
    
    // Thêm phương thức để tìm kiếm theo tên với phân trang
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
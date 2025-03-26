package com.techzenacademy.TechFinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.techzenacademy.TechFinance.entity.Supplier;

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
    
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:phone IS NULL OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
           "(:address IS NULL OR LOWER(s.address) LIKE LOWER(CONCAT('%', :address, '%'))) AND " +
           "(:taxCode IS NULL OR LOWER(s.taxCode) LIKE LOWER(CONCAT('%', :taxCode, '%'))) AND " +
           "(:isActive IS NULL OR s.isActive = :isActive)")
    Page<Supplier> findWithFiltersPageable(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("address") String address,
            @Param("taxCode") String taxCode,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
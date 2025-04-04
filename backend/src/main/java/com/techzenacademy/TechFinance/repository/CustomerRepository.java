package com.techzenacademy.TechFinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.techzenacademy.TechFinance.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    List<Customer> findByIsActiveTrue();
    boolean existsByEmail(String email);
    boolean existsByIdentification(String identification);
    Optional<Customer> findByEmail(String email);
    
    @Query("SELECT c FROM Customer c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:phone IS NULL OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
           "(:address IS NULL OR LOWER(c.address) LIKE LOWER(CONCAT('%', :address, '%'))) AND " +
           "(:identification IS NULL OR LOWER(c.identification) LIKE LOWER(CONCAT('%', :identification, '%'))) AND " +
           "(:taxCode IS NULL OR LOWER(c.taxCode) LIKE LOWER(CONCAT('%', :taxCode, '%'))) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive)")
    List<Customer> findWithFilters(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("address") String address,
            @Param("identification") String identification,
            @Param("taxCode") String taxCode,
            @Param("isActive") Boolean isActive);
            
    // Thêm phương thức phân trang
    @Query("SELECT c FROM Customer c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:phone IS NULL OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
           "(:address IS NULL OR LOWER(c.address) LIKE LOWER(CONCAT('%', :address, '%'))) AND " +
           "(:identification IS NULL OR LOWER(c.identification) LIKE LOWER(CONCAT('%', :identification, '%'))) AND " +
           "(:taxCode IS NULL OR LOWER(c.taxCode) LIKE LOWER(CONCAT('%', :taxCode, '%'))) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive)")
    Page<Customer> findWithFiltersPageable(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("address") String address,
            @Param("identification") String identification,
            @Param("taxCode") String taxCode,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
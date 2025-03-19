package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Integer> {
    List<IncomeCategory> findByIsActiveTrue();
    boolean existsByName(String name);
    
    @Query("SELECT i FROM IncomeCategory i WHERE " + 
           "(:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:isActive IS NULL OR i.isActive = :isActive)")
    Page<IncomeCategory> findWithFilters(
            @Param("name") String name, 
            @Param("isActive") Boolean isActive, 
            Pageable pageable);
}
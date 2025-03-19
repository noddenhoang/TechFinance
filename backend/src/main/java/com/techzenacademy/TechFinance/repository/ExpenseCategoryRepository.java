package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {
    List<ExpenseCategory> findByIsActiveTrue();
    boolean existsByName(String name);
    
    // Thêm phương thức tìm kiếm có hỗ trợ phân trang
    @Query("SELECT e FROM ExpenseCategory e WHERE " + 
           "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:isActive IS NULL OR e.isActive = :isActive)")
    Page<ExpenseCategory> findWithFilters(
            @Param("name") String name, 
            @Param("isActive") Boolean isActive, 
            Pageable pageable);
}
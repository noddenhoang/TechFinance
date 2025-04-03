package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeBudget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IncomeBudgetRepository extends JpaRepository<IncomeBudget, Integer> {
    List<IncomeBudget> findByYearAndMonth(Integer year, Integer month);
    
    Page<IncomeBudget> findByYearAndMonth(Integer year, Integer month, Pageable pageable);
    
    List<IncomeBudget> findByYear(Integer year);
    
    Page<IncomeBudget> findByYear(Integer year, Pageable pageable);
    
    List<IncomeBudget> findByCategoryId(Integer categoryId);
    
    Page<IncomeBudget> findByCategoryId(Integer categoryId, Pageable pageable);
    
    Optional<IncomeBudget> findByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);
    
    boolean existsByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);

    @Query("SELECT SUM(b.amount) FROM IncomeBudget b WHERE b.year = :year AND b.month = :month")
    Optional<BigDecimal> sumAmountByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    Page<IncomeBudget> findByCategoryIdAndYear(Integer categoryId, Integer year, Pageable pageable);
}
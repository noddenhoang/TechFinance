package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IncomeBudgetRepository extends JpaRepository<IncomeBudget, Integer> {
    // ... existing methods ...
    
    @Query("SELECT SUM(b.amount) FROM IncomeBudget b WHERE b.year = :year AND b.month = :month")
    Optional<BigDecimal> sumAmountByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}
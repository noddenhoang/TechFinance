package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, Integer> {
    List<ExpenseBudget> findByYearAndMonth(Integer year, Integer month);
    
    List<ExpenseBudget> findByYear(Integer year);
    
    List<ExpenseBudget> findByCategoryId(Integer categoryId);
    
    Optional<ExpenseBudget> findByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);
    
    boolean existsByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);

    @Query("SELECT SUM(b.amount) FROM ExpenseBudget b WHERE b.year = :year AND b.month = :month")
    Optional<BigDecimal> sumAmountByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}
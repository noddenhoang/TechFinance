package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, Integer> {
    
    List<ExpenseTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<ExpenseTransaction> findByCategoryIdOrderByTransactionDateDesc(Integer categoryId);
    
    List<ExpenseTransaction> findBySupplierIdOrderByTransactionDateDesc(Integer supplierId);
    
    @Query("SELECT et FROM ExpenseTransaction et WHERE YEAR(et.transactionDate) = :year AND MONTH(et.transactionDate) = :month ORDER BY et.transactionDate DESC")
    List<ExpenseTransaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
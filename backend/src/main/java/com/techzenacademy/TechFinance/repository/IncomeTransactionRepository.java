package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Integer> {
    
    List<IncomeTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<IncomeTransaction> findByCategoryIdOrderByTransactionDateDesc(Integer categoryId);
    
    List<IncomeTransaction> findByCustomerIdOrderByTransactionDateDesc(Integer customerId);
    
    @Query("SELECT it FROM IncomeTransaction it WHERE YEAR(it.transactionDate) = :year AND MONTH(it.transactionDate) = :month ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
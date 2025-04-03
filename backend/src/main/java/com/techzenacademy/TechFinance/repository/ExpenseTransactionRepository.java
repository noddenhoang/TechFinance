package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, Integer> {
    
    // Phương thức tìm kiếm với phân trang và nhiều bộ lọc
    @Query("SELECT et FROM ExpenseTransaction et " +
           "WHERE (:supplierId IS NULL OR et.supplier.id = :supplierId) " +
           "AND (:categoryId IS NULL OR et.category.id = :categoryId) " +
           "AND (:startDate IS NULL OR et.transactionDate >= :startDate) " + 
           "AND (:endDate IS NULL OR et.transactionDate <= :endDate) " +
           "AND (:minAmount IS NULL OR et.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR et.amount <= :maxAmount) " +
           "AND (:paymentStatus IS NULL OR et.paymentStatus = :paymentStatus) " +
           "AND (:referenceNo IS NULL OR LOWER(et.referenceNo) LIKE LOWER(CONCAT('%', :referenceNo, '%')))")
    Page<ExpenseTransaction> findByFilters(
            @Param("supplierId") Integer supplierId,
            @Param("categoryId") Integer categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("referenceNo") String referenceNo,
            Pageable pageable);
    
    // Giữ lại các phương thức hiện có
    List<ExpenseTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<ExpenseTransaction> findByCategoryIdOrderByTransactionDateDesc(Integer categoryId);
    
    List<ExpenseTransaction> findBySupplierIdOrderByTransactionDateDesc(Integer supplierId);
    
    @Query("SELECT et FROM ExpenseTransaction et WHERE YEAR(et.transactionDate) = :year AND MONTH(et.transactionDate) = :month ORDER BY et.transactionDate DESC")
    List<ExpenseTransaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    Page<ExpenseTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
        LocalDate startDate, LocalDate endDate, Pageable pageable);
}
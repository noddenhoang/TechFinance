package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.IncomeTransaction.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Integer> {
    
    // Phương thức tìm kiếm với phân trang và nhiều bộ lọc
    @Query("SELECT it FROM IncomeTransaction it " +
           "WHERE (:customerId IS NULL OR it.customer.id = :customerId) " +
           "AND (:categoryId IS NULL OR it.category.id = :categoryId) " +
           "AND (:startDate IS NULL OR it.transactionDate >= :startDate) " + 
           "AND (:endDate IS NULL OR it.transactionDate <= :endDate) " +
           "AND (:minAmount IS NULL OR it.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR it.amount <= :maxAmount) " +
           "AND (:paymentStatus IS NULL OR it.paymentStatus = :paymentStatus) " +
           "AND (:referenceNo IS NULL OR LOWER(it.referenceNo) LIKE LOWER(CONCAT('%', :referenceNo, '%')))")
    Page<IncomeTransaction> findByFilters(
            @Param("customerId") Integer customerId,
            @Param("categoryId") Integer categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("referenceNo") String referenceNo,
            Pageable pageable);
    
    // Giữ lại các phương thức hiện có
    List<IncomeTransaction> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate startDate, LocalDate endDate);
    
    List<IncomeTransaction> findByCategoryIdOrderByTransactionDateDesc(Integer categoryId);
    
    List<IncomeTransaction> findByCustomerIdOrderByTransactionDateDesc(Integer customerId);
    
    @Query("SELECT it FROM IncomeTransaction it WHERE YEAR(it.transactionDate) = :year AND MONTH(it.transactionDate) = :month ORDER BY it.transactionDate DESC")
    List<IncomeTransaction> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
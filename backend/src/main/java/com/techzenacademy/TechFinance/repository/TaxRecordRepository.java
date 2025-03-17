package com.techzenacademy.TechFinance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.techzenacademy.TechFinance.entity.TaxRecord;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRecordRepository extends JpaRepository<TaxRecord, Integer> {
    
    Optional<TaxRecord> findByYearAndMonth(Integer year, Integer month);
    
    List<TaxRecord> findByYearOrderByMonth(Integer year);
    
    @Query(value = "CALL sp_generate_tax_report(:year, :month)", nativeQuery = true)
    List<Object[]> generateTaxReport(@Param("year") Integer year, @Param("month") Integer month);
}
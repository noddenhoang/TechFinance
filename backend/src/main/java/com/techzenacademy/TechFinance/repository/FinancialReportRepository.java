package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialReportRepository extends JpaRepository<FinancialReport, Integer> {
    
    Optional<FinancialReport> findByYearAndMonthAndReportType(Integer year, Integer month, FinancialReport.ReportType reportType);
    
    List<FinancialReport> findByYearAndReportTypeOrderByMonth(Integer year, FinancialReport.ReportType reportType);
    
    @Query("SELECT fr FROM FinancialReport fr WHERE fr.year = :year AND fr.month = :month")
    List<FinancialReport> findAllByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
    
    @Query(value = "CALL sp_generate_monthly_report(:year, :month)", nativeQuery = true)
    List<Object[]> generateMonthlyReport(@Param("year") Integer year, @Param("month") Integer month);
    
    @Query(value = "CALL sp_income_by_category(:year, :month)", nativeQuery = true)
    List<Object[]> getIncomeByCategory(@Param("year") Integer year, @Param("month") Integer month);
    
    @Query(value = "CALL sp_expense_by_category(:year, :month)", nativeQuery = true)
    List<Object[]> getExpenseByCategory(@Param("year") Integer year, @Param("month") Integer month);
}
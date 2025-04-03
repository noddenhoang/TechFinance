package com.techzenacademy.TechFinance.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.techzenacademy.TechFinance.entity.MonthlyReport;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    List<MonthlyReport> findByUserIdOrderByReportDateDesc(Long userId);
    List<MonthlyReport> findByUserIdAndReportDateBetweenOrderByReportDateAsc(Long userId, LocalDate startDate, LocalDate endDate);
} 
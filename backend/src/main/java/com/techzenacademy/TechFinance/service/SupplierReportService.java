package com.techzenacademy.TechFinance.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public interface SupplierReportService {
    Map<String, Object> generateMonthlyReport(int year, int month);
    Map<String, Object> generateQuarterlyReport(int year, int quarter);
    Map<String, Object> generateYearlyReport(int year);
} 
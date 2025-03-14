package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Integer> {
    List<IncomeCategory> findByIsActiveTrue();
    boolean existsByName(String name);
}
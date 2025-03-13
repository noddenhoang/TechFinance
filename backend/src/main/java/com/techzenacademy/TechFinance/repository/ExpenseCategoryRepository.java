package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {
    List<ExpenseCategory> findByIsActiveTrue();
    boolean existsByName(String name);
}
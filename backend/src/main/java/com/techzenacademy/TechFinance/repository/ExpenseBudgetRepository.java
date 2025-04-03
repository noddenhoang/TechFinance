package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, Integer> {
    List<ExpenseBudget> findByYear(Integer year);
    List<ExpenseBudget> findByYearAndMonth(Integer year, Integer month);
    Page<ExpenseBudget> findByYear(Integer year, Pageable pageable);
    Page<ExpenseBudget> findByYearAndMonth(Integer year, Integer month, Pageable pageable);
    List<ExpenseBudget> findByCategoryId(Integer categoryId);
    Page<ExpenseBudget> findByCategoryIdAndYear(Integer categoryId, Integer year, Pageable pageable);
    Optional<ExpenseBudget> findByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);
    boolean existsByCategoryIdAndYearAndMonth(Integer categoryId, Integer year, Integer month);
}
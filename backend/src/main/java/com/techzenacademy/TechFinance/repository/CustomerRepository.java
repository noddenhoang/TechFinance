package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    List<Customer> findByIsActiveTrue();
    boolean existsByEmail(String email);
    Optional<Customer> findByEmail(String email);
}
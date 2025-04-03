package com.techzenacademy.TechFinance.repository;

import com.techzenacademy.TechFinance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByIdentification(String identification);
}
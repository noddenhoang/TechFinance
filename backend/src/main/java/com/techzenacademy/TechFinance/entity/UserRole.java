package com.techzenacademy.TechFinance.entity;

public enum UserRole {
    admin,
    user;
    
    public String getName() {
        return this.name();
    }
}
package com.techzenacademy.TechFinance.dto;

import lombok.Data;

@Data
public class ExpenseCategoryDTO {
    private Integer id;
    private String name;
    private String description;
    private Boolean isActive;
}
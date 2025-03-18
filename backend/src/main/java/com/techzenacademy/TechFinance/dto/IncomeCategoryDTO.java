package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IncomeCategoryDTO {
    private Integer id;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
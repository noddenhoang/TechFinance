package com.techzenacademy.TechFinance.dto;

import lombok.Data;

@Data
public class CustomerDTO {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String identification;
    private String address;
    private String taxCode;
    private String notes;
    private Boolean isActive;
}
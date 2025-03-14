package com.techzenacademy.TechFinance.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryReportDTO {
    private List<CategoryBreakdownDTO> incomeCategories;
    private List<CategoryBreakdownDTO> expenseCategories;
}

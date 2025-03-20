package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.ExpenseBudgetDTO;
import com.techzenacademy.TechFinance.dto.ExpenseBudgetRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.ExpenseBudget;
import com.techzenacademy.TechFinance.entity.ExpenseCategory;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.ExpenseBudgetRepository;
import com.techzenacademy.TechFinance.repository.ExpenseCategoryRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseBudgetService {
    
    @Autowired
    private ExpenseBudgetRepository budgetRepository;
    
    @Autowired
    private ExpenseCategoryRepository categoryRepository;
    
    @Autowired
    private ExpenseTransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<ExpenseBudgetDTO> getAllBudgets() {
        return budgetRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public PageResponse<ExpenseBudgetDTO> getPagedBudgets(Integer year, Integer month, Integer categoryId, Pageable pageable) {
        // Nếu năm là null, sử dụng năm hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        // Chỉ cập nhật tự động ngân sách nếu tháng được chỉ định cụ thể
        if (month != null) {
            // Đảm bảo ngân sách được cập nhật trước khi trả về
            refreshBudgets(year, month);
        } else {
            // Khi month=null, cần đảm bảo tất cả tháng trong năm đều được cập nhật
            for (int m = 1; m <= 12; m++) {
                refreshBudgets(year, m);
            }
        }
        
        Page<ExpenseBudget> budgetPage;
        
        if (categoryId != null) {
            if (month != null) {
                // Trường hợp có cả year, month và categoryId
                Optional<ExpenseBudget> budget = budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month);
                if (budget.isPresent()) {
                    List<ExpenseBudget> budgetList = new ArrayList<>();
                    budgetList.add(budget.get());
                    budgetPage = new PageImpl<>(budgetList, pageable, 1);
                } else {
                    budgetPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
                }
            } else {
                // Trường hợp có year và categoryId, nhưng month là null
                // Lấy tất cả các tháng của category đó trong năm đó
                budgetPage = budgetRepository.findByCategoryIdAndYear(categoryId, year, pageable);
            }
        } else if (month != null) {
            // Trường hợp có year và month, không có categoryId
            budgetPage = budgetRepository.findByYearAndMonth(year, month, pageable);
        } else {
            // Trường hợp chỉ có year, không có month và categoryId
            // Lấy tất cả ngân sách trong năm đó
            budgetPage = budgetRepository.findByYear(year, pageable);
        }
        
        // Chuyển đổi Page<ExpenseBudget> sang Page<ExpenseBudgetDTO>
        Page<ExpenseBudgetDTO> dtoPage = budgetPage.map(this::mapToDTO);
        
        // Tạo PageResponse từ Page<ExpenseBudgetDTO>
        return new PageResponse<>(dtoPage);
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByYearAndMonth(Integer year, Integer month) {
        return budgetRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByYear(Integer year) {
        return budgetRepository.findByYear(year).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseBudgetDTO> getBudgetsByCategory(Integer categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Expense category not found with id: " + categoryId));
                
        return budgetRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public ExpenseBudgetDTO getBudgetById(Integer id) {
        return budgetRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense budget not found with id: " + id));
    }
    
    public ExpenseBudgetDTO getBudgetByCategoryAndPeriod(Integer categoryId, Integer year, Integer month) {
        return budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Expense budget not found for category id: %d, year: %d, month: %d", 
                                  categoryId, year, month)));
    }
    
    /**
     * Cập nhật ngân sách chi tiêu dựa trên chi tiêu thực tế
     * @param year Năm cần cập nhật (null = năm hiện tại)
     * @param month Tháng cần cập nhật (null = tháng hiện tại)
     */
    @Transactional
    public void refreshBudgets(Integer year, Integer month) {
        // Nếu không có năm và tháng được cung cấp, sử dụng tháng hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }
        
        // Tính toán ngày đầu và cuối tháng
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        
        // Lấy tất cả danh mục chi tiêu
        List<ExpenseCategory> categories = categoryRepository.findByIsActiveTrue();
        
        // Lấy tất cả giao dịch chi tiêu trong tháng
        List<ExpenseTransaction> transactions = transactionRepository
            .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        // Tính tổng chi tiêu theo danh mục - CHỈ TÍNH CÁC GIAO DỊCH ĐÃ THANH TOÁN
        Map<Integer, BigDecimal> categoryTotals = new HashMap<>();
        for (ExpenseTransaction transaction : transactions) {
            if (transaction.getPaymentStatus() == ExpenseTransaction.PaymentStatus.PAID) {
                Integer categoryId = transaction.getCategory().getId();
                BigDecimal currentAmount = categoryTotals.getOrDefault(categoryId, BigDecimal.ZERO);
                categoryTotals.put(categoryId, currentAmount.add(transaction.getAmount()));
            }
        }
        
        User currentUser = getCurrentUser();
        
        // Cập nhật hoặc tạo mới ngân sách cho mỗi danh mục
        for (ExpenseCategory category : categories) {
            Integer categoryId = category.getId();
            BigDecimal amount = categoryTotals.getOrDefault(categoryId, BigDecimal.ZERO);
            
            // Tìm ngân sách hiện có
            Optional<ExpenseBudget> existingBudget = budgetRepository
                .findByCategoryIdAndYearAndMonth(categoryId, year, month);
            
            if (existingBudget.isPresent()) {
                // Cập nhật ngân sách hiện có
                ExpenseBudget budget = existingBudget.get();
                budget.setAmount(amount);
                budgetRepository.save(budget);
            } else {
                // Tạo ngân sách mới
                ExpenseBudget newBudget = new ExpenseBudget();
                newBudget.setCategory(category);
                newBudget.setYear(year);
                newBudget.setMonth(month);
                newBudget.setAmount(amount);
                newBudget.setNotes("Tự động tính toán từ chi tiêu đã thanh toán");
                newBudget.setCreatedBy(currentUser);
                budgetRepository.save(newBudget);
            }
        }
    }
    
    private ExpenseBudgetDTO mapToDTO(ExpenseBudget budget) {
        ExpenseBudgetDTO dto = new ExpenseBudgetDTO();
        dto.setId(budget.getId());
        dto.setCategoryId(budget.getCategory().getId());
        dto.setCategoryName(budget.getCategory().getName());
        dto.setYear(budget.getYear());
        dto.setMonth(budget.getMonth());
        dto.setAmount(budget.getAmount());
        dto.setNotes(budget.getNotes());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
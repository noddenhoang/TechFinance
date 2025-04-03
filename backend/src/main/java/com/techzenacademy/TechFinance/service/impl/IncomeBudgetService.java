package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.IncomeBudgetDTO;
import com.techzenacademy.TechFinance.dto.IncomeBudgetRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.entity.IncomeBudget;
import com.techzenacademy.TechFinance.entity.IncomeCategory;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.IncomeBudgetRepository;
import com.techzenacademy.TechFinance.repository.IncomeCategoryRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
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
public class IncomeBudgetService {
    
    @Autowired
    private IncomeBudgetRepository budgetRepository;
    
    @Autowired
    private IncomeCategoryRepository categoryRepository;
    
    @Autowired
    private IncomeTransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<IncomeBudgetDTO> getAllBudgets() {
        return budgetRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public PageResponse<IncomeBudgetDTO> getPagedBudgets(Integer year, Integer month, Integer categoryId, Pageable pageable) {
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
        
        Page<IncomeBudget> budgetPage;
        
        if (categoryId != null) {
            if (month != null) {
                // Trường hợp có cả year, month và categoryId
                Optional<IncomeBudget> budget = budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month);
                if (budget.isPresent()) {
                    List<IncomeBudget> budgetList = new ArrayList<>();
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
        
        // Chuyển đổi Page<IncomeBudget> sang Page<IncomeBudgetDTO>
        Page<IncomeBudgetDTO> dtoPage = budgetPage.map(this::mapToDTO);
        
        // Tạo PageResponse từ Page<IncomeBudgetDTO>
        return new PageResponse<>(dtoPage);
    }
    
    public List<IncomeBudgetDTO> getBudgetsByYearAndMonth(Integer year, Integer month) {
        return budgetRepository.findByYearAndMonth(year, month).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeBudgetDTO> getBudgetsByYear(Integer year) {
        return budgetRepository.findByYear(year).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<IncomeBudgetDTO> getBudgetsByCategory(Integer categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + categoryId));
                
        return budgetRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public IncomeBudgetDTO getBudgetById(Integer id) {
        return budgetRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Income budget not found with id: " + id));
    }
    
    public IncomeBudgetDTO getBudgetByCategoryAndPeriod(Integer categoryId, Integer year, Integer month) {
        return budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Income budget not found for category id: %d, year: %d, month: %d", 
                                  categoryId, year, month)));
    }
    
    @Transactional
    public IncomeBudgetDTO createBudget(IncomeBudgetRequest request) {
        try {
            // Mã hiện tại
            if (budgetRepository.existsByCategoryIdAndYearAndMonth(
                    request.getCategoryId(), request.getYear(), request.getMonth())) {
                throw new IllegalArgumentException(
                    String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                                request.getCategoryId(), request.getYear(), request.getMonth()));
            }
            
            IncomeBudget budget = new IncomeBudget();
            updateBudgetFromRequest(budget, request);
            budget.setCreatedBy(getCurrentUser());
            
            IncomeBudget savedBudget = budgetRepository.save(budget);
            return mapToDTO(savedBudget);
        } catch (Exception e) {
            // Log chi tiết lỗi
            e.printStackTrace();
            throw new RuntimeException("Failed to create income budget: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public IncomeBudgetDTO updateBudget(Integer id, IncomeBudgetRequest request) {
        IncomeBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Income budget not found with id: " + id));
        
        // Check if we need to check for duplicate (category, year, month) combination
        boolean needsUniqueCheck = false;
        
        if (request.getCategoryId() != null && !budget.getCategory().getId().equals(request.getCategoryId())) {
            needsUniqueCheck = true;
        }
        
        if (request.getYear() != null && !budget.getYear().equals(request.getYear())) {
            needsUniqueCheck = true;
        }
        
        if (request.getMonth() != null && !budget.getMonth().equals(request.getMonth())) {
            needsUniqueCheck = true;
        }
        
        // If category, year or month changed, check for duplicates
        if (needsUniqueCheck) {
            Integer categoryId = request.getCategoryId() != null ? request.getCategoryId() : budget.getCategory().getId();
            Integer year = request.getYear() != null ? request.getYear() : budget.getYear();
            Integer month = request.getMonth() != null ? request.getMonth() : budget.getMonth();
            
            Optional<IncomeBudget> existingBudget = budgetRepository.findByCategoryIdAndYearAndMonth(categoryId, year, month);
            if (existingBudget.isPresent() && !existingBudget.get().getId().equals(id)) {
                throw new IllegalArgumentException(
                    String.format("A budget for category id: %d, year: %d, month: %d already exists", 
                                  categoryId, year, month));
            }
        }
        
        // Update only provided fields
        if (request.getCategoryId() != null) {
            IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
            budget.setCategory(category);
        }
        
        if (request.getYear() != null) {
            budget.setYear(request.getYear());
        }
        
        if (request.getMonth() != null) {
            budget.setMonth(request.getMonth());
        }
        
        if (request.getAmount() != null) {
            budget.setAmount(request.getAmount());
        }
        
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        IncomeBudget updatedBudget = budgetRepository.save(budget);
        return mapToDTO(updatedBudget);
    }
    
    @Transactional
    public void deleteBudget(Integer id) {
        if (!budgetRepository.existsById(id)) {
            throw new EntityNotFoundException("Income budget not found with id: " + id);
        }
        budgetRepository.deleteById(id);
    }
    
    /**
     * Cập nhật ngân sách thu nhập dựa trên thu nhập thực tế
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
        
        // Lấy tất cả danh mục thu nhập
        List<IncomeCategory> categories = categoryRepository.findByIsActiveTrue();
        
        // Lấy tất cả giao dịch thu nhập trong tháng
        List<IncomeTransaction> transactions = transactionRepository
            .findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
        
        // Tính tổng thu nhập theo danh mục - CHỈ TÍNH CÁC GIAO DỊCH ĐÃ THANH TOÁN
        Map<Integer, BigDecimal> categoryTotals = new HashMap<>();
        for (IncomeTransaction transaction : transactions) {
            if (transaction.getPaymentStatus() == IncomeTransaction.PaymentStatus.RECEIVED) {
                Integer categoryId = transaction.getCategory().getId();
                BigDecimal currentAmount = categoryTotals.getOrDefault(categoryId, BigDecimal.ZERO);
                categoryTotals.put(categoryId, currentAmount.add(transaction.getAmount()));
            }
        }
        
        User currentUser = getCurrentUser();
        
        // Cập nhật hoặc tạo mới ngân sách cho mỗi danh mục
        for (IncomeCategory category : categories) {
            Integer categoryId = category.getId();
            BigDecimal amount = categoryTotals.getOrDefault(categoryId, BigDecimal.ZERO);
            
            // Tìm ngân sách hiện có
            Optional<IncomeBudget> existingBudget = budgetRepository
                .findByCategoryIdAndYearAndMonth(categoryId, year, month);
            
            if (existingBudget.isPresent()) {
                // Cập nhật ngân sách hiện có
                IncomeBudget budget = existingBudget.get();
                budget.setAmount(amount);
                budgetRepository.save(budget);
            } else {
                // Tạo ngân sách mới
                IncomeBudget newBudget = new IncomeBudget();
                newBudget.setCategory(category);
                newBudget.setYear(year);
                newBudget.setMonth(month);
                newBudget.setAmount(amount);
                newBudget.setNotes("Tự động tính toán từ thu nhập đã nhận");
                newBudget.setCreatedBy(currentUser);
                budgetRepository.save(newBudget);
            }
        }
    }
    
    private void updateBudgetFromRequest(IncomeBudget budget, IncomeBudgetRequest request) {
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        
        IncomeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Income category not found with id: " + request.getCategoryId()));
        budget.setCategory(category);
        
        if (request.getYear() == null) {
            throw new IllegalArgumentException("Year cannot be null");
        }
        budget.setYear(request.getYear());
        
        if (request.getMonth() == null) {
            throw new IllegalArgumentException("Month cannot be null");
        }
        budget.setMonth(request.getMonth());
        
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        budget.setAmount(request.getAmount());
        
        budget.setNotes(request.getNotes() != null ? request.getNotes() : "");
    }
    
    private IncomeBudgetDTO mapToDTO(IncomeBudget budget) {
        IncomeBudgetDTO dto = new IncomeBudgetDTO();
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
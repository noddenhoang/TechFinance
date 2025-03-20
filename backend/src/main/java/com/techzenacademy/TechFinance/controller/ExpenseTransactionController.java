package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.ExpenseTransactionDTO;
import com.techzenacademy.TechFinance.dto.ExpenseTransactionRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.service.impl.ExpenseTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expense-transactions")
public class ExpenseTransactionController {
    
    @Autowired
    private ExpenseTransactionService expenseTransactionService;
    
    @GetMapping
    public ResponseEntity<PageResponse<ExpenseTransactionDTO>> getTransactions(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "supplierId", required = false) Integer supplierId,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(name = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(name = "referenceNo", required = false) String referenceNo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "transactionDate,desc") String[] sort) {

        // Xử lý sắp xếp
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "desc";

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Nếu id được chỉ định, chỉ trả về giao dịch có id đó
        if (id != null) {
            PageResponse<ExpenseTransactionDTO> response = expenseTransactionService.getTransactionById(id, pageable);
            return ResponseEntity.ok(response);
        }

        // Xử lý khoảng thời gian không đầy đủ
        if (startDate != null && endDate == null) {
            // Nếu chỉ có startDate, lấy đến hiện tại
            endDate = LocalDate.now();
        } else if (startDate == null && endDate != null) {
            // Nếu chỉ có endDate, lấy từ xa nhất có thể
            startDate = LocalDate.of(2000, 1, 1); // Hoặc một ngày xa trong quá khứ
        }

        // Gọi service để lấy dữ liệu với phân trang và tất cả các bộ lọc
        PageResponse<ExpenseTransactionDTO> result = expenseTransactionService.getFilteredTransactions(
                supplierId, categoryId, startDate, endDate,
                minAmount, maxAmount, paymentStatus,
                referenceNo, pageable);

        return ResponseEntity.ok(result);
    }

    // Giữ lại các API hiện có cho tương thích ngược
    @GetMapping("/date-range")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByDateRange(startDate, endDate));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByCategory(@PathVariable("categoryId") Integer categoryId) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByCategory(categoryId));
    }
    
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsBySupplier(@PathVariable("supplierId") Integer supplierId) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsBySupplier(supplierId));
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<ExpenseTransactionDTO>> getTransactionsByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionsByMonth(year, month));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseTransactionDTO> getTransactionById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(expenseTransactionService.getTransactionById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ExpenseTransactionDTO> createTransaction(@Valid @RequestBody ExpenseTransactionRequest request) {
        return new ResponseEntity<>(expenseTransactionService.createTransaction(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ExpenseTransactionDTO> updateTransaction(
            @PathVariable("id") Integer id,
            @RequestBody ExpenseTransactionRequest request) {
        return ResponseEntity.ok(expenseTransactionService.updateTransaction(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Integer id) {
        expenseTransactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
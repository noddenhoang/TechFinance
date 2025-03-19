package com.techzenacademy.TechFinance.controller;

import com.techzenacademy.TechFinance.dto.IncomeTransactionDTO;
import com.techzenacademy.TechFinance.dto.IncomeTransactionRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.service.impl.IncomeTransactionService;
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
@RequestMapping("/api/income-transactions")
public class IncomeTransactionController {

    @Autowired
    private IncomeTransactionService incomeTransactionService;

    @GetMapping
    public ResponseEntity<PageResponse<IncomeTransactionDTO>> getTransactions(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "customerId", required = false) Integer customerId,
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
            PageResponse<IncomeTransactionDTO> response = incomeTransactionService.getTransactionById(id, pageable);
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
        PageResponse<IncomeTransactionDTO> result = incomeTransactionService.getFilteredTransactions(
                customerId, categoryId, startDate, endDate,
                minAmount, maxAmount, paymentStatus,
                referenceNo, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * API lấy một giao dịch theo ID
     * Giữ lại để tương thích ngược
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncomeTransactionDTO> getTransactionById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(incomeTransactionService.getTransactionById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<IncomeTransactionDTO> createTransaction(@Valid @RequestBody IncomeTransactionRequest request) {
        return new ResponseEntity<>(incomeTransactionService.createTransaction(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<IncomeTransactionDTO> updateTransaction(
            @PathVariable("id") Integer id,
            @RequestBody IncomeTransactionRequest request) {
        return ResponseEntity.ok(incomeTransactionService.updateTransaction(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") Integer id) {
        incomeTransactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
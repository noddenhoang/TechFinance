package com.techzenacademy.TechFinance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.dto.CustomerDTO;
import com.techzenacademy.TechFinance.dto.CustomerRequest;
import com.techzenacademy.TechFinance.dto.page.PageResponse;
import com.techzenacademy.TechFinance.service.impl.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CustomerDTO>> getCustomers(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "identification", required = false) String identification,
            @RequestParam(name = "taxCode", required = false) String taxCode,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") String[] sort) {
        
        // Xử lý sắp xếp
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sortField);
        
        // Tạo đối tượng Pageable
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        // Lấy dữ liệu đã phân trang
        PageResponse<CustomerDTO> pagedResponse = 
                customerService.getPagedCustomers(name, email, phone, address, identification, taxCode, isActive, pageable);
                
        return ResponseEntity.ok(pagedResponse);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")  
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable("id") Integer id, 
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
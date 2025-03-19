package com.techzenacademy.TechFinance.service.impl;

import com.techzenacademy.TechFinance.dto.CustomerDTO;
import com.techzenacademy.TechFinance.dto.CustomerRequest;
import com.techzenacademy.TechFinance.entity.Customer;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.repository.CustomerRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.techzenacademy.TechFinance.dto.page.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CustomerDTO> getActiveCustomers() {
        return customerRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public CustomerDTO getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));
    }
    
    public CustomerDTO createCustomer(CustomerRequest request) {
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A customer with this email already exists");
        }
        
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setTaxCode(request.getTaxCode());
        customer.setNotes(request.getNotes());
        customer.setIsActive(request.getIsActive());
        customer.setCreatedBy(getCurrentUser());
        
        Customer savedCustomer = customerRepository.save(customer);
        return mapToDTO(savedCustomer);
    }
    
    public CustomerDTO updateCustomer(Integer id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));
        
        // Check if email is changed and if new email already exists
        if (request.getEmail() != null && 
                !request.getEmail().equals(customer.getEmail()) && 
                customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A customer with this email already exists");
        }
        
        // Only update fields if they are provided in the request
        if (request.getName() != null) {
            customer.setName(request.getName());
        }
        
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        
        if (request.getTaxCode() != null) {
            customer.setTaxCode(request.getTaxCode());
        }
        
        if (request.getNotes() != null) {
            customer.setNotes(request.getNotes());
        }
        
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
        
        Customer updatedCustomer = customerRepository.save(customer);
        return mapToDTO(updatedCustomer);
    }
    
    public void deleteCustomer(Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
    }
    
    private CustomerDTO mapToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setTaxCode(customer.getTaxCode());
        dto.setNotes(customer.getNotes());
        dto.setIsActive(customer.getIsActive());
        return dto;
    }
    
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
    
    /**
     * Filter customers based on optional criteria
     * 
     * @param name Filter by name containing this string (case-insensitive)
     * @param email Filter by email containing this string (case-insensitive)
     * @param phone Filter by phone containing this string (case-insensitive)
     * @param address Filter by address containing this string (case-insensitive)
     * @param isActive Filter by active status
     * @return List of filtered customers
     */
    public List<CustomerDTO> filterCustomers(String name, String email, String phone, String address, Boolean isActive) {
        return customerRepository.findWithFilters(name, email, phone, address, isActive)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Filter customers with pagination
     * 
     * @param name Filter by name containing this string (case-insensitive)
     * @param email Filter by email containing this string (case-insensitive)
     * @param phone Filter by phone containing this string (case-insensitive)
     * @param address Filter by address containing this string (case-insensitive)
     * @param isActive Filter by active status
     * @param pageable Pagination information
     * @return PageResponse of filtered customers
     */
    public PageResponse<CustomerDTO> getPagedCustomers(
            String name, String email, String phone, String address, Boolean isActive, Pageable pageable) {
        
        Page<Customer> customerPage = customerRepository.findWithFiltersPageable(
                name, email, phone, address, isActive, pageable);
        
        // Map the contents using the existing mapToDTO method
        Page<CustomerDTO> dtoPage = customerPage.map(this::mapToDTO);
        
        // Return the custom page response
        return new PageResponse<>(dtoPage);
    }
}
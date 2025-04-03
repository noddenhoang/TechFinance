package com.techzenacademy.TechFinance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.techzenacademy.TechFinance.entity.ChatConversation;
import com.techzenacademy.TechFinance.entity.PredictionInput;

/**
 * Repository để truy vấn và thao tác với dữ liệu đầu vào dự đoán
 */
public interface PredictionInputRepository extends JpaRepository<PredictionInput, Integer> {
    
    /**
     * Tìm dữ liệu đầu vào dự đoán mới nhất cho một cuộc hội thoại
     */
    Optional<PredictionInput> findTopByConversationOrderByCreatedAtDesc(ChatConversation conversation);
    
    /**
     * Xóa tất cả dữ liệu đầu vào của một cuộc hội thoại
     */
    void deleteByConversation(ChatConversation conversation);
} 
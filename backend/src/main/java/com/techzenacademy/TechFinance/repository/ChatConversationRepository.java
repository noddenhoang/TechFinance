package com.techzenacademy.TechFinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.techzenacademy.TechFinance.entity.ChatConversation;
import com.techzenacademy.TechFinance.entity.User;

/**
 * Repository để truy vấn và thao tác với cuộc hội thoại chat
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    
    /**
     * Tìm tất cả cuộc hội thoại của một người dùng, sắp xếp theo thời gian tạo giảm dần
     */
    List<ChatConversation> findByUser(User user);
    
    /**
     * Tìm tất cả cuộc hội thoại của một người dùng, sắp xếp theo thời gian cập nhật giảm dần
     */
    List<ChatConversation> findByUserOrderByLastUpdatedAtDesc(User user);
    
    /**
     * Tìm các cuộc hội thoại theo loại, sắp xếp theo thời gian tạo giảm dần
     */
    List<ChatConversation> findByTypeOrderByCreatedAtDesc(ChatConversation.ConversationType type);
    
    /**
     * Tìm các cuộc hội thoại đang hoạt động của một người dùng
     */
    List<ChatConversation> findByUserAndIsActiveTrue(User user);
    
    /**
     * Tìm cuộc hội thoại theo ID và User
     */
    Optional<ChatConversation> findByIdAndUser(String id, User user);
} 
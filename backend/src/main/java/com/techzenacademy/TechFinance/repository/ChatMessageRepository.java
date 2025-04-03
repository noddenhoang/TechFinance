package com.techzenacademy.TechFinance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.techzenacademy.TechFinance.entity.ChatConversation;
import com.techzenacademy.TechFinance.entity.ChatMessage;
import com.techzenacademy.TechFinance.entity.ChatMessage.MessageSender;

/**
 * Repository để truy vấn và thao tác với tin nhắn chat
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Tìm tất cả tin nhắn của một cuộc hội thoại, sắp xếp theo thời gian tăng dần
     */
    List<ChatMessage> findByConversationOrderByTimestampAsc(ChatConversation conversation);
    
    /**
     * Tìm tin nhắn của một cuộc hội thoại theo người gửi, sắp xếp theo thời gian tăng dần
     */
    List<ChatMessage> findByConversationAndSenderOrderByTimestampAsc(ChatConversation conversation, MessageSender sender);
    
    /**
     * Tìm tin nhắn đầu tiên của một cuộc hội thoại
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = ?1 AND m.sender = 'USER' ORDER BY m.timestamp ASC LIMIT 1")
    List<ChatMessage> findFirstMessageByConversation(String conversationId);
    
    /**
     * Đếm số tin nhắn trong một cuộc hội thoại
     */
    long countByConversation(ChatConversation conversation);
    
    /**
     * Xóa tất cả tin nhắn của một cuộc hội thoại
     */
    @Modifying
    void deleteByConversation(ChatConversation conversation);
} 
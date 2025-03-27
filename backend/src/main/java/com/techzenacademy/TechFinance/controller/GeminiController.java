package com.techzenacademy.TechFinance.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techzenacademy.TechFinance.dto.gemini.ChatConversationDTO;
import com.techzenacademy.TechFinance.dto.gemini.GeminiRequestDTO;
import com.techzenacademy.TechFinance.dto.gemini.GeminiResponseDTO;
import com.techzenacademy.TechFinance.dto.gemini.PredictionInput;
import com.techzenacademy.TechFinance.service.impl.GeminiService;

/**
 * Controller for Gemini AI API
 */
@RestController
@RequestMapping("/api/gemini")
public class GeminiController {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);
    
    @Autowired
    private GeminiService geminiService;
    
    /**
     * General chat endpoint
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody GeminiRequestDTO request) {
        try {
            logger.info("Received chat request: {}", request.getUserMessage());
            GeminiResponseDTO response = geminiService.chat(request);
            
            if (!response.isSuccess()) {
                logger.warn("Chat request failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            GeminiResponseDTO errorResponse = new GeminiResponseDTO();
            errorResponse.setSuccess(false);
            errorResponse.setError("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Revenue prediction endpoint
     */
    @PostMapping("/predict-revenue")
    public ResponseEntity<?> predictRevenue(@RequestBody PredictionInput input) {
        try {
            logger.info("Received revenue prediction request for time period: {}", input.getTimePeriod());
            GeminiResponseDTO response = geminiService.predictRevenue(input);
            
            if (!response.isSuccess()) {
                logger.warn("Revenue prediction request failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing revenue prediction request", e);
            GeminiResponseDTO errorResponse = new GeminiResponseDTO();
            errorResponse.setSuccess(false);
            errorResponse.setError("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get all conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        try {
            logger.info("Fetching user conversations");
            List<ChatConversationDTO> conversations = geminiService.getUserConversations();
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            logger.error("Error fetching conversations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An error occurred while fetching conversations: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific conversation
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<?> getConversation(@PathVariable("id") String id) {
        try {
            logger.info("Fetching conversation with ID: {}", id);
            
            if (id == null || id.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Conversation ID cannot be empty"));
            }
            
            GeminiResponseDTO response = geminiService.getConversation(id);
            
            if (!response.isSuccess()) {
                logger.warn("Conversation retrieval failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching conversation with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An error occurred while retrieving the conversation: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a conversation
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable("id") String id) {
        try {
            logger.info("Deleting conversation with ID: {}", id);
            
            if (id == null || id.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Conversation ID cannot be empty"));
            }
            
            boolean success = geminiService.deleteConversation(id);
            
            if (!success) {
                logger.warn("Conversation deletion failed for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conversation not found or you don't have permission to delete it"));
            }
            
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Conversation deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting conversation with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An error occurred while deleting the conversation: " + e.getMessage()));
        }
    }
    
    private GeminiResponseDTO createErrorResponse(String errorMessage) {
        GeminiResponseDTO response = new GeminiResponseDTO();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }
} 
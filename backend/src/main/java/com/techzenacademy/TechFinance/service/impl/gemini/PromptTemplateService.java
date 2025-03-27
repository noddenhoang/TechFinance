package com.techzenacademy.TechFinance.service.impl.gemini;

import org.springframework.stereotype.Service;

/**
 * Service for managing prompt templates for Gemini API.
 */
@Service
public class PromptTemplateService {

    /**
     * Universal prompt template for all use cases
     */
    private static final String UNIVERSAL_TEMPLATE = 
        "Bạn là một trợ lý AI chuyên gia tài chính, dựa vào yêu cầu sau của người dùng [user_input] và dữ liệu sau từ database [data_database] hãy thực hiện yêu cầu của người dùng";

    /**
     * Create a prompt for any user query
     * 
     * @param userInput User message/query
     * @param databaseData Data retrieved from the database (can be null)
     * @return Completed prompt
     */
    public String createPrompt(String userInput, String databaseData) {
        if (databaseData == null || databaseData.isEmpty()) {
            databaseData = "Không có dữ liệu từ database.";
        }
        
        return UNIVERSAL_TEMPLATE
                .replace("user_input", userInput)
                .replace("data_database", databaseData);
    }
} 
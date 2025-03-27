package com.techzenacademy.TechFinance.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techzenacademy.TechFinance.dto.gemini.ChatConversationDTO;
import com.techzenacademy.TechFinance.dto.gemini.GeminiMessageDTO;
import com.techzenacademy.TechFinance.dto.gemini.GeminiRequestDTO;
import com.techzenacademy.TechFinance.dto.gemini.GeminiResponseDTO;
import com.techzenacademy.TechFinance.dto.gemini.PredictionInput;
import com.techzenacademy.TechFinance.entity.ChatConversation;
import com.techzenacademy.TechFinance.entity.ChatMessage;
import com.techzenacademy.TechFinance.entity.MonthlyReport;
import com.techzenacademy.TechFinance.entity.User;
import com.techzenacademy.TechFinance.entity.IncomeTransaction;
import com.techzenacademy.TechFinance.entity.ExpenseTransaction;
import com.techzenacademy.TechFinance.repository.ChatConversationRepository;
import com.techzenacademy.TechFinance.repository.ChatMessageRepository;
import com.techzenacademy.TechFinance.repository.MonthlyReportRepository;
import com.techzenacademy.TechFinance.repository.PredictionInputRepository;
import com.techzenacademy.TechFinance.repository.UserRepository;
import com.techzenacademy.TechFinance.repository.IncomeTransactionRepository;
import com.techzenacademy.TechFinance.repository.ExpenseTransactionRepository;
import com.techzenacademy.TechFinance.service.impl.gemini.PromptTemplateService;

import jakarta.annotation.PostConstruct;

/**
 * Service to interact with Google Gemini API.
 */
@Service
public class GeminiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private RestTemplate restTemplate;
    
    @Autowired
    private PromptTemplateService promptTemplateService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatConversationRepository conversationRepository;
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    @Autowired
    private PredictionInputRepository predictionInputRepository;
    
    @Autowired
    private FinancialReportService financialReportService;
    
    @Autowired
    private MonthlyReportRepository monthlyReportRepository;
    
    @Autowired
    private IncomeTransactionRepository incomeTransactionRepository;
    
    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.project.id}")
    private String projectId;
    
    @Value("${gemini.model.id}")
    private String modelId;
    
    @Value("${gemini.region}")
    private String region;
    
    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        logger.info("Gemini service initialized with API key: {}", maskApiKey(apiKey));
    }
    
    /**
     * Get current user from security context
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<User> user = userRepository.findByUsername(username);
            return user.orElse(null);
        }
        return null;
    }
    
    /**
     * Handle chat request for general queries
     */
    @Transactional
    public GeminiResponseDTO chat(GeminiRequestDTO request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return createErrorResponse("User authentication failed");
            }
            
            ChatConversation conversation;
            boolean isNewConversation = false;
            
            // Find or create conversation
            if (request.getConversationId() != null) {
                Optional<ChatConversation> existingConversation = 
                    conversationRepository.findById(request.getConversationId());
                
                if (existingConversation.isPresent() && existingConversation.get().getUser().getId().equals(currentUser.getId())) {
                    conversation = existingConversation.get();
                } else {
                    return createErrorResponse("Conversation not found");
                }
            } else {
                conversation = new ChatConversation();
                conversation.setId(UUID.randomUUID().toString());
                conversation.setUser(currentUser);
                conversation.setType(ChatConversation.ConversationType.GENERAL);
                isNewConversation = true;
            }
            
            // Save user message
            ChatMessage userMessage = new ChatMessage();
            userMessage.setConversation(conversation);
            userMessage.setContent(request.getUserMessage());
            userMessage.setSender(ChatMessage.MessageSender.USER);
            
            if (isNewConversation) {
                conversationRepository.save(conversation);
            }
            messageRepository.save(userMessage);
            
            // Get database data for this user
            String databaseData = getRelevantDataForUser(currentUser, request.getUserMessage());
            
            // Create prompt with the universal template
            String prompt = promptTemplateService.createPrompt(request.getUserMessage(), databaseData);
            
            // TODO: Call actual Gemini API
            // For now, simulate responses
            String response = simulateGeminiResponse(prompt);
            
            // Save AI message
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setConversation(conversation);
            aiMessage.setContent(response);
            aiMessage.setSender(ChatMessage.MessageSender.AI);
            messageRepository.save(aiMessage);
            
            return createSuccessResponse(response, conversation.getId(), buildMessagesList(conversation));
            
        } catch (Exception e) {
            logger.error("Error in chat with Gemini API", e);
            return createErrorResponse("Error chatting with AI: " + e.getMessage());
        }
    }
    
    /**
     * Revenue prediction function
     */
    @Transactional
    public GeminiResponseDTO predictRevenue(PredictionInput input) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return createErrorResponse("User authentication failed");
            }
            
            ChatConversation conversation;
            boolean isNewConversation = false;
            
            // Find or create conversation
            if (input.getConversationId() != null) {
                Optional<ChatConversation> existingConversation = 
                    conversationRepository.findById(input.getConversationId());
                
                if (existingConversation.isPresent() && existingConversation.get().getUser().getId().equals(currentUser.getId())) {
                    conversation = existingConversation.get();
                } else {
                    return createErrorResponse("Conversation not found");
                }
            } else {
                conversation = new ChatConversation();
                conversation.setId(UUID.randomUUID().toString());
                conversation.setUser(currentUser);
                conversation.setType(ChatConversation.ConversationType.REVENUE_PREDICTION);
                isNewConversation = true;
                conversationRepository.save(conversation);
            }
            
            // Save user message as business data
            ChatMessage userMessage = new ChatMessage();
            userMessage.setConversation(conversation);
            userMessage.setContent(input.getBusinessData());
            userMessage.setSender(ChatMessage.MessageSender.USER);
            messageRepository.save(userMessage);
            
            // Get historical data if available
            List<MonthlyReport> reports = monthlyReportRepository.findByUserIdOrderByReportDateDesc(currentUser.getId().longValue());
            String historicalData = "";
            
            if (!reports.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Historical revenue data:\n");
                for (MonthlyReport report : reports) {
                    sb.append(report.getReportDate().format(DateTimeFormatter.ofPattern("MM/yyyy")))
                      .append(": ")
                      .append(report.getRevenue())
                      .append("\n");
                }
                historicalData = sb.toString();
            }
            
            // Get historical income transaction data
            List<IncomeTransaction> incomeTransactions = incomeTransactionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "transactionDate"));
            
            if (!incomeTransactions.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Historical income transaction data:\n");
                
                // Group transactions by month for more useful trend analysis
                Map<YearMonth, BigDecimal> monthlyIncome = new HashMap<>();
                
                for (IncomeTransaction transaction : incomeTransactions) {
                    YearMonth yearMonth = YearMonth.from(transaction.getTransactionDate());
                    BigDecimal currentTotal = monthlyIncome.getOrDefault(yearMonth, BigDecimal.ZERO);
                    monthlyIncome.put(yearMonth, currentTotal.add(transaction.getAmount()));
                }
                
                // Sort by date and format
                List<YearMonth> sortedMonths = new ArrayList<>(monthlyIncome.keySet());
                Collections.sort(sortedMonths);
                
                for (YearMonth month : sortedMonths) {
                    sb.append(month.format(DateTimeFormatter.ofPattern("MM/yyyy")))
                      .append(": ")
                      .append(monthlyIncome.get(month))
                      .append("\n");
                }
                
                historicalData = sb.toString();
            }
            
            // Create prompt with the universal template
            String userInput = "Predict revenue for " + input.getTimePeriod() + " based on the following business information: " + input.getBusinessData();
            String prompt = promptTemplateService.createPrompt(userInput, historicalData);
            
            // TODO: Call actual Gemini API
            // For now, simulate responses
            String response = simulateRevenuePrediction(prompt);
            
            // Save AI response
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setConversation(conversation);
            aiMessage.setContent(response);
            aiMessage.setSender(ChatMessage.MessageSender.AI);
            messageRepository.save(aiMessage);
            
            List<GeminiMessageDTO> messages = buildMessagesList(conversation);
            return createSuccessResponse(response, conversation.getId(), messages);
            
        } catch (Exception e) {
            logger.error("Error in revenue prediction", e);
            return createErrorResponse("Error predicting revenue: " + e.getMessage());
        }
    }
    
    /**
     * Get relevant data from the database based on the user's query
     */
    private String getRelevantDataForUser(User user, String userQuery) {
        StringBuilder data = new StringBuilder();
        
        // Determine how many transactions to fetch based on query content
        int limit = 20; // Default limit
        if (userQuery.toLowerCase().contains("detailed") || 
            userQuery.toLowerCase().contains("all transactions") ||
            userQuery.toLowerCase().contains("comprehensive")) {
            limit = 50; // More data for detailed requests
        }
        
        // Determine date range - default to last 3 months if not specified
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);
        
        // Look for time-related keywords in the query
        if (userQuery.toLowerCase().contains("year") || 
            userQuery.toLowerCase().contains("annual") || 
            userQuery.toLowerCase().contains("yearly")) {
            startDate = endDate.minusYears(1);
        } else if (userQuery.toLowerCase().contains("month") || 
                  userQuery.toLowerCase().contains("monthly")) {
            startDate = endDate.minusMonths(1);
        }
        
        // Fetch income transactions
        Page<IncomeTransaction> incomeTransactions = incomeTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
            startDate, endDate, PageRequest.of(0, limit));
        
        // Fetch expense transactions
        Page<ExpenseTransaction> expenseTransactions = expenseTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
            startDate, endDate, PageRequest.of(0, limit));
            
        // Add income transactions data
        if (incomeTransactions.hasContent()) {
            data.append("Income Transactions:\n");
            for (IncomeTransaction transaction : incomeTransactions.getContent()) {
                data.append(transaction.getTransactionDate())
                    .append(" | Category: ").append(transaction.getCategory().getName())
                    .append(" | Amount: ").append(transaction.getAmount())
                    .append(" | Status: ").append(transaction.getPaymentStatus().getValue());
                
                if (transaction.getCustomer() != null) {
                    data.append(" | Customer: ").append(transaction.getCustomer().getName());
                }
                
                if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                    data.append(" | Note: ").append(transaction.getDescription());
                }
                
                data.append("\n");
            }
            data.append("\n");
        }
        
        // Add expense transactions data
        if (expenseTransactions.hasContent()) {
            data.append("Expense Transactions:\n");
            for (ExpenseTransaction transaction : expenseTransactions.getContent()) {
                data.append(transaction.getTransactionDate())
                    .append(" | Category: ").append(transaction.getCategory().getName())
                    .append(" | Amount: ").append(transaction.getAmount())
                    .append(" | Status: ").append(transaction.getPaymentStatus().getValue());
                
                if (transaction.getSupplier() != null) {
                    data.append(" | Supplier: ").append(transaction.getSupplier().getName());
                }
                
                if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                    data.append(" | Note: ").append(transaction.getDescription());
                }
                
                data.append("\n");
            }
            data.append("\n");
        }
        
        // Calculate summary statistics
        if (incomeTransactions.hasContent() || expenseTransactions.hasContent()) {
            data.append("Summary Statistics:\n");
            
            // Total income
            BigDecimal totalIncome = BigDecimal.ZERO;
            for (IncomeTransaction transaction : incomeTransactions.getContent()) {
                totalIncome = totalIncome.add(transaction.getAmount());
            }
            data.append("Total Income: ").append(totalIncome).append("\n");
            
            // Total expense
            BigDecimal totalExpense = BigDecimal.ZERO;
            for (ExpenseTransaction transaction : expenseTransactions.getContent()) {
                totalExpense = totalExpense.add(transaction.getAmount());
            }
            data.append("Total Expense: ").append(totalExpense).append("\n");
            
            // Net profit
            BigDecimal netProfit = totalIncome.subtract(totalExpense);
            data.append("Net Profit: ").append(netProfit).append("\n");
        }
        
        // Add monthly reports data for context (keeping this for historical trends)
        List<MonthlyReport> reports = monthlyReportRepository.findByUserIdOrderByReportDateDesc(user.getId().longValue());
        if (!reports.isEmpty()) {
            data.append("\nMonthly Reports:\n");
            for (MonthlyReport report : reports) {
                data.append(report.getReportDate().format(DateTimeFormatter.ofPattern("MM/yyyy")))
                    .append(" - Revenue: ").append(report.getRevenue())
                    .append(", Expenses: ").append(report.getExpenses())
                    .append(", Profit: ").append(report.getProfit())
                    .append("\n");
            }
        }
        
        return data.toString();
    }
    
    /**
     * Get user conversations
     */
    public List<ChatConversationDTO> getUserConversations() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ArrayList<>();
        }
        
        List<ChatConversation> conversations = conversationRepository.findByUserOrderByLastUpdatedAtDesc(currentUser);
        List<ChatConversationDTO> result = new ArrayList<>();
        
        for (ChatConversation convo : conversations) {
            ChatConversationDTO dto = new ChatConversationDTO();
            dto.setId(convo.getId());
            dto.setType(convo.getType().name());
            dto.setCreatedAt(convo.getCreatedAt());
            
            // Get first user message
            List<ChatMessage> messages = messageRepository.findByConversationAndSenderOrderByTimestampAsc(
                convo, ChatMessage.MessageSender.USER);
            
            if (!messages.isEmpty()) {
                ChatMessage firstMessage = messages.get(0);
                dto.setFirstMessage(firstMessage.getContent());
            }
            
            // Get message count
            long messageCount = messageRepository.countByConversation(convo);
            dto.setMessageCount((int) messageCount);
            
            result.add(dto);
        }
        
        return result;
    }
    
    /**
     * Get specific conversation
     */
    public GeminiResponseDTO getConversation(String conversationId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return createErrorResponse("User authentication failed");
            }
            
            if (conversationId == null || conversationId.trim().isEmpty()) {
                return createErrorResponse("Conversation ID cannot be empty");
            }
            
            Optional<ChatConversation> conversationOpt = conversationRepository.findById(conversationId);
            
            if (!conversationOpt.isPresent()) {
                return createErrorResponse("Conversation not found");
            }
            
            ChatConversation conversation = conversationOpt.get();
            
            // Check if conversation has a valid user
            if (conversation.getUser() == null) {
                logger.error("Found conversation without associated user: {}", conversationId);
                return createErrorResponse("Invalid conversation data");
            }
            
            // Safe comparison for user ownership
            if (!conversation.getUser().getId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access conversation {} belonging to user {}", 
                    currentUser.getUsername(), conversationId, conversation.getUser().getUsername());
                return createErrorResponse("You don't have permission to access this conversation");
            }
            
            List<GeminiMessageDTO> messages = buildMessagesList(conversation);
            
            GeminiResponseDTO response = new GeminiResponseDTO();
            response.setSuccess(true);
            response.setConversationId(conversationId);
            response.setMessages(messages);
            return response;
        } catch (Exception e) {
            logger.error("Error retrieving conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse("An error occurred while retrieving the conversation: " + e.getMessage());
        }
    }
    
    /**
     * Delete conversation
     */
    @Transactional
    public boolean deleteConversation(String conversationId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        Optional<ChatConversation> conversationOpt = conversationRepository.findById(conversationId);
        
        if (!conversationOpt.isPresent() || !conversationOpt.get().getUser().getId().equals(currentUser.getId())) {
            return false;
        }
        
        ChatConversation conversation = conversationOpt.get();
        
        // Delete all messages first
        messageRepository.deleteByConversation(conversation);
        
        // Then delete the conversation
        conversationRepository.delete(conversation);
        
        return true;
    }
    
    /**
     * Build list of messages for a conversation
     */
    private List<GeminiMessageDTO> buildMessagesList(ChatConversation conversation) {
        try {
            if (conversation == null) {
                logger.error("Attempted to build message list for null conversation");
                return new ArrayList<>();
            }
            
            List<ChatMessage> messages = messageRepository.findByConversationOrderByTimestampAsc(conversation);
            List<GeminiMessageDTO> result = new ArrayList<>();
            
            for (ChatMessage message : messages) {
                if (message == null) continue;
                
                GeminiMessageDTO dto = new GeminiMessageDTO();
                dto.setId(message.getId() != null ? message.getId().longValue() : 0L);
                dto.setContent(message.getContent() != null ? message.getContent() : "");
                dto.setUser(message.getSender() == ChatMessage.MessageSender.USER);
                
                // Handle timestamp safely with correct type
                if (message.getTimestamp() != null) {
                    dto.setTimestamp(message.getTimestamp());
                } else {
                    dto.setTimestamp(java.time.LocalDateTime.now());
                }
                
                result.add(dto);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error building message list: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // Simulate responses for development - replace with actual API calls
    private String simulateGeminiResponse(String prompt) {
        try {
            // Gọi thực tế đến API Gemini
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến dịch vụ Gemini: " + e.getMessage());
        }
    }
    
    private String simulateRevenuePrediction(String prompt) {
        try {
            // Gọi thực tế đến API Gemini với prompt đặc biệt cho dự đoán doanh thu
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            logger.error("Error calling Gemini API for revenue prediction: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến dịch vụ dự đoán doanh thu: " + e.getMessage());
        }
    }
    
    /**
     * Gọi API Gemini sử dụng HTTP trực tiếp
     */
    private String callGeminiAPI(String prompt) throws IOException {
        logger.info("Calling Gemini API with prompt length: {} characters", prompt.length());
        
        // Kiểm tra API key có sẵn không
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API key không được cấu hình");
        }
        
        // Sử dụng API trực tiếp thay vì sử dụng thư viện
        RestTemplate restTemplate = new RestTemplate();
        
        // Chuẩn bị URL với API key và model ID từ cấu hình
        String url = String.format("https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s", 
            modelId, apiKey);
        
        logger.info("Using Gemini model: {}", modelId);
        
        // Chuẩn bị headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Tạo JSON request sử dụng một chuỗi
        String requestBody = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}],\"generationConfig\":{\"temperature\":0.7,\"topK\":40,\"topP\":0.95,\"maxOutputTokens\":2048}}",
            prompt.replace("\"", "\\\"").replace("\n", "\\n"));
        
        // Gửi request
        try {
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // Xử lý response
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                
                // Sử dụng Jackson để xử lý JSON
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);
                
                // Trích xuất nội dung text
                String result = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                
                if (result != null && !result.isEmpty()) {
                    logger.info("Received response from Gemini API: {} characters", result.length());
                    return result;
                } else {
                    throw new IOException("Không thể tìm thấy text trong phản hồi API");
                }
            } else {
                throw new IOException("Lỗi khi gọi API: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error in Gemini API call: {}", e.getMessage(), e);
            
            // Nếu là lỗi 404 và đang sử dụng model không phải gemini-pro, thử với gemini-pro
            if (e.getMessage() != null && e.getMessage().contains("404") && !modelId.equals("gemini-pro")) {
                logger.warn("Model {} not found. Attempting fallback to gemini-pro...", modelId);
                String fallbackUrl = String.format("https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent?key=%s", apiKey);
                
                try {
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(fallbackUrl, entity, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        String responseBody = response.getBody();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(responseBody);
                        
                        String result = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                        
                        if (result != null && !result.isEmpty()) {
                            logger.info("Received response from fallback model gemini-pro: {} characters", result.length());
                            return result;
                        }
                    }
                } catch (Exception fallbackError) {
                    logger.error("Fallback to gemini-pro also failed: {}", fallbackError.getMessage());
                }
            }
            
            throw new IOException("Lỗi khi gọi Gemini API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Che giấu API key trong logs
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
    
    private GeminiResponseDTO createSuccessResponse(String response, String conversationId, List<GeminiMessageDTO> messages) {
        GeminiResponseDTO responseDTO = new GeminiResponseDTO();
        responseDTO.setSuccess(true);
        responseDTO.setResponse(response);
        responseDTO.setConversationId(conversationId);
        responseDTO.setMessages(messages);
        return responseDTO;
    }
    
    private GeminiResponseDTO createErrorResponse(String errorMessage) {
        GeminiResponseDTO responseDTO = new GeminiResponseDTO();
        responseDTO.setSuccess(false);
        responseDTO.setError(errorMessage);
        return responseDTO;
    }
}
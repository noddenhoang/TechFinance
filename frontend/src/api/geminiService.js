import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Gemini API service
 */
const geminiService = {
  /**
   * Send chat message
   * @param {string} message - Message content
   * @param {string} conversationId - Conversation ID (null for new conversation)
   * @returns {Promise} API response
   */
  sendChatMessage(message, conversationId = null) {
    const requestData = {
      userMessage: message,
      conversationId: conversationId
    };
    
    return axios.post(`${API_URL}/api/gemini/chat`, requestData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },
  
  /**
   * Send revenue prediction request
   * @param {string} businessData - Business information
   * @param {string} timePeriod - Time period for prediction
   * @param {string} conversationId - Conversation ID (null for new conversation)
   * @returns {Promise} API response
   */
  sendRevenuePrediction(businessData, timePeriod, conversationId = null) {
    const requestData = {
      businessData: businessData,
      timePeriod: timePeriod,
      conversationId: conversationId
    };
    
    return axios.post(`${API_URL}/api/gemini/predict-revenue`, requestData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },
  
  /**
   * Get conversation list
   * @returns {Promise} Conversation list
   */
  getConversations() {
    return axios.get(`${API_URL}/api/gemini/conversations`, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },
  
  /**
   * Get conversation details
   * @param {string} conversationId - Conversation ID
   * @returns {Promise} Conversation details
   */
  getConversation(id) {
    return axios.get(`${API_URL}/api/gemini/conversations/${id}`, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },
  
  /**
   * Delete conversation
   * @param {string} conversationId - Conversation ID
   * @returns {Promise} Delete result
   */
  deleteConversation(id) {
    return axios.delete(`${API_URL}/api/gemini/conversations/${id}`, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }
};

// Thêm interceptor để xử lý lỗi từ API
axios.interceptors.response.use(
  response => response,
  error => {
    // Lấy thông báo lỗi từ response
    let errorMessage = "Không thể kết nối đến máy chủ";
    
    if (error.response) {
      // Server trả về response với status code nằm ngoài phạm vi 2xx
      if (error.response.data) {
        if (error.response.data.error) {
          errorMessage = error.response.data.error;
        } else if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (typeof error.response.data === 'string') {
          errorMessage = error.response.data;
        }
      }
      
      // Set HTTP status code for better error handling
      error.statusCode = error.response.status;
    } else if (error.request) {
      // Request đã được tạo nhưng không nhận được response
      errorMessage = "Không nhận được phản hồi từ máy chủ";
    } else {
      // Có lỗi khi cấu hình request
      errorMessage = error.message || "Đã xảy ra lỗi";
    }
    
    // Log errors to console with details
    console.error('API Error:', {
      message: errorMessage,
      originalError: error,
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status
    });
    
    // Đưa thông tin lỗi vào error để component có thể sử dụng
    error.displayMessage = errorMessage;
    
    return Promise.reject(error);
  }
);

export default geminiService; 
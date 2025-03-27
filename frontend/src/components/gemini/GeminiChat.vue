<template>
  <div class="gemini-chat">
    <div class="chat-header">
      <h3>Trợ lý AI</h3>
    </div>
    
    <div class="chat-messages" ref="chatContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="index" 
        :class="['message', message.sender === 'user' ? 'user-message' : 'ai-message']"
      >
        <div class="message-content">
          <div class="message-avatar">
            <div class="avatar-circle" :class="{ 'user-avatar': message.sender === 'user' }">
              <span v-if="message.sender === 'user'">U</span>
              <span v-else>AI</span>
            </div>
          </div>
          <div class="message-text" v-html="formatMessage(message.content)"></div>
        </div>
        <div class="message-time">{{ formatTime(message.timestamp) }}</div>
      </div>
      
      <div v-if="isLoading" class="message ai-message loading-message">
        <div class="message-content">
          <div class="message-avatar">
            <div class="avatar-circle">
              <span>AI</span>
            </div>
          </div>
          <div class="message-text typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <textarea 
        v-model="messageContent" 
        @keydown.enter.prevent="handleEnterKey"
        placeholder="Nhập tin nhắn của bạn..."
        :disabled="isLoading"
        rows="1"
        ref="messageInput"
        @input="autoResizeTextarea"
      ></textarea>
      <button 
        @click="sendMessage" 
        :disabled="isLoading || !messageContent.trim()"
        class="send-btn"
        title="Gửi tin nhắn"
      >
        <i class="bi bi-send-fill"></i>
      </button>
    </div>
  </div>
</template>

<script>
import geminiService from '@/api/geminiService';

export default {
  name: 'GeminiChat',
  props: {
    chatType: {
      type: String,
      required: true,
      validator: (value) => ['general', 'revenue'].includes(value)
    },
    conversationId: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      messageContent: '',
      messages: [],
      isLoading: false,
      conversationLoaded: false
    };
  },
  watch: {
    conversationId: {
      immediate: true,
      handler(newId) {
        if (newId) {
          this.loadConversation(newId);
        } else {
          this.resetChat();
        }
      }
    }
  },
  mounted() {
    this.addWelcomeMessage();
  },
  updated() {
    this.scrollToBottom();
  },
  methods: {
    getWelcomeMessage() {
      switch (this.chatType) {
        case 'general':
          return 'Xin chào! Tôi là trợ lý AI tài chính của TechFinance. Tôi có thể giúp bạn trả lời các câu hỏi về tài chính, phân tích dữ liệu kinh doanh, và đưa ra dự báo. Bạn cần hỗ trợ gì hôm nay?';
        case 'revenue':
          return 'Chào mừng đến với tính năng dự đoán doanh thu! Vui lòng cung cấp thông tin về doanh nghiệp của bạn như: lĩnh vực kinh doanh, quy mô, thị trường mục tiêu và các yếu tố mùa vụ (nếu có).';
        default:
          return 'Xin chào! Tôi có thể giúp gì cho bạn?';
      }
    },
    
    formatMessage(content) {
      if (!content) return '';
      
      // Convert line breaks to <br> tags
      let formatted = content.replace(/\n/g, '<br>');
      
      // Bold text between ** symbols
      formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      
      // Italic text between * symbols
      formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');
      
      // Process lists
      const lines = formatted.split('<br>');
      let inList = false;
      let formattedLines = [];
      
      for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        
        // Check for bullet points
        if (line.match(/^- /)) {
          if (!inList) {
            formattedLines.push('<ul>');
            inList = true;
          }
          formattedLines.push('<li>' + line.substring(2) + '</li>');
        } 
        // Check for numbered lists
        else if (line.match(/^\d+\. /)) {
          if (!inList) {
            formattedLines.push('<ol>');
            inList = true;
          }
          formattedLines.push('<li>' + line.replace(/^\d+\. /, '') + '</li>');
        } 
        else {
          if (inList) {
            formattedLines.push(inList === 'ul' ? '</ul>' : '</ol>');
            inList = false;
          }
          formattedLines.push(line);
        }
      }
      
      if (inList) {
        formattedLines.push(inList === 'ul' ? '</ul>' : '</ol>');
      }
      
      return formattedLines.join('<br>');
    },
    
    addWelcomeMessage() {
      this.messages = [{
        content: this.getWelcomeMessage(),
        sender: 'ai',
        timestamp: new Date()
      }];
    },
    
    resetChat() {
      this.messages = [];
      this.conversationLoaded = false;
      this.addWelcomeMessage();
    },
    
    async loadConversation(id) {
      try {
        this.isLoading = true;
        const response = await geminiService.getConversation(id);
        console.log('Raw response from server:', response.data);
        
        if (response.data && response.data.messages) {
          console.log('Messages from server:', response.data.messages);
          
          this.messages = response.data.messages.map(msg => {
            // Sử dụng hàm determineIfUserMessage để xác định người gửi
            const isUserMessage = this.determineIfUserMessage(msg);
            
            // Log thông tin chi tiết để debug
            console.log(`Message: "${msg.content?.substring(0, 20)}..." | isUser: ${msg.isUser} | Result: ${isUserMessage ? 'USER' : 'AI'}`);
            console.log('Full message object:', JSON.stringify(msg, null, 2));
            
            return {
              content: msg.content || '',
              sender: isUserMessage ? 'user' : 'ai',
              timestamp: new Date(msg.timestamp || new Date())
            };
          });
          
          console.log('Processed messages:', this.messages);
          this.conversationLoaded = true;
        } else {
          this.resetChat();
        }
      } catch (error) {
        console.error('Lỗi khi tải hội thoại:', error);
        
        // Extract the error message to display to the user
        let errorMessage = 'Không thể tải cuộc hội thoại. Vui lòng thử lại sau.';
        
        if (error.displayMessage) {
          errorMessage = error.displayMessage;
        }
        
        // Show error message to user
        this.$emit('error', errorMessage);
        
        // If the conversation doesn't exist or cannot be accessed, reset the chat
        this.resetChat();
        
        // Add an error message to the chat
        this.messages.push({
          content: `<strong>Lỗi:</strong> ${errorMessage}`,
          sender: 'ai',
          timestamp: new Date()
        });
      } finally {
        this.isLoading = false;
      }
    },
    
    handleEnterKey(event) {
      // Allow new line with shift+enter
      if (event.shiftKey) {
        return true;
      }
      
      // Send message with just enter
      this.sendMessage();
      return false;
    },
    
    async sendMessage() {
      if (!this.messageContent.trim()) return;
      
      // Store message content before clearing
      const sentContent = this.messageContent.trim();
      
      // Create user message
      const userMessage = {
        content: sentContent,
        sender: 'user',
        timestamp: new Date()
      };
      
      // Add user message to the list
      this.messages.push(userMessage);
      
      // Clear input
      this.messageContent = '';
      this.autoResizeTextarea();
      
      try {
        this.isLoading = true;
        let response;
        
        if (this.chatType === 'general') {
          response = await geminiService.sendChatMessage(sentContent, this.conversationId);
        } else if (this.chatType === 'revenue') {
          response = await geminiService.sendRevenuePrediction(
            sentContent,  // Business data
            "next quarter", // Default time period 
            this.conversationId
          );
        }
        
        console.log('Send message response:', response.data);
        
        // Update conversation ID if needed
        if (response.data.conversationId && !this.conversationId) {
          this.$emit('conversation-loaded', response.data.conversationId);
          this.conversationLoaded = true;
        }
        
        // Update messages from response
        if (response.data.messages && response.data.messages.length > 0) {
          console.log('Got messages response:', response.data.messages);
          
          // Create a new array to hold processed messages
          let newMessages = [];
          
          // Process messages from the server
          for (const msg of response.data.messages) {
            // Sử dụng hàm determineIfUserMessage để xác định người gửi
            const isUserMessage = this.determineIfUserMessage(msg);
            
            console.log(`Processing message: "${msg.content?.substring(0, 20)}..." | isUser: ${msg.isUser} | Result: ${isUserMessage ? 'USER' : 'AI'}`);
            console.log('Full message object:', JSON.stringify(msg, null, 2));
            
            newMessages.push({
              content: msg.content || '',
              sender: isUserMessage ? 'user' : 'ai',
              timestamp: new Date(msg.timestamp || new Date())
            });
          }
          
          // Replace messages array with processed messages
          this.messages = newMessages;
          console.log('Updated messages array:', this.messages);
        } else if (response.data.response) {
          // If API only returns a single response, keep the user message and add AI response
          this.messages = [
            userMessage,
            {
              content: response.data.response,
              sender: 'ai',
              timestamp: new Date()
            }
          ];
        }
      } catch (error) {
        console.error('Lỗi khi gửi tin nhắn:', error);
        
        // Extract the error message from the response if available
        let errorMessage = 'Có lỗi xảy ra khi xử lý yêu cầu của bạn. Vui lòng thử lại sau.';
        
        if (error.displayMessage) {
          // Use the display message set by the axios interceptor
          errorMessage = error.displayMessage;
        } else if (error.response) {
          // The request was made and the server responded with an error status
          if (error.response.data && error.response.data.error) {
            errorMessage = error.response.data.error;
          } else if (error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
          } else if (typeof error.response.data === 'string') {
            errorMessage = error.response.data;
          }
        } else if (error.message) {
          // The error has a message property
          errorMessage = error.message;
        }
        
        this.messages.push({
          content: `<strong>Lỗi:</strong> ${errorMessage}`,
          sender: 'ai',
          timestamp: new Date()
        });
      } finally {
        this.isLoading = false;
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },
    
    formatTime(timestamp) {
      const date = new Date(timestamp);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    },
    
    scrollToBottom() {
      if (this.$refs.chatContainer) {
        this.$refs.chatContainer.scrollTop = this.$refs.chatContainer.scrollHeight;
      }
    },
    
    autoResizeTextarea() {
      const textarea = this.$refs.messageInput;
      if (!textarea) return;
      
      // Reset height to auto to get correct scrollHeight
      textarea.style.height = 'auto';
      
      // Set height based on content, with maximum height
      const maxHeight = 120; // Maximum height in pixels
      const newHeight = Math.min(textarea.scrollHeight, maxHeight);
      textarea.style.height = newHeight + 'px';
      
      // Add scrollbar if exceeds maximum height
      textarea.style.overflowY = textarea.scrollHeight > maxHeight ? 'auto' : 'hidden';
    },

    determineIfUserMessage(message) {
      // Null/undefined check
      if (!message) return false;
      
      console.log('Checking if message is from user:', message);
      
      // Kiểm tra thuộc tính isUser trực tiếp
      if (typeof message.isUser !== 'undefined') {
        // Boolean check
        if (typeof message.isUser === 'boolean') {
          return message.isUser;
        }
        
        // String check
        if (typeof message.isUser === 'string') {
          const lowerValue = message.isUser.toLowerCase();
          return lowerValue === 'true' || lowerValue === 'user' || lowerValue === '1';
        }
        
        // Number check
        if (typeof message.isUser === 'number') {
          return message.isUser === 1;
        }
      }
      
      // Kiểm tra thuộc tính sender nếu có
      if (typeof message.sender !== 'undefined') {
        if (typeof message.sender === 'string') {
          const upperSender = message.sender.toUpperCase();
          return upperSender === 'USER' || upperSender === 'NGƯỜI DÙNG' || upperSender === 'U';
        }
      }
      
      // Kiểm tra các thuộc tính khác có thể xác định người gửi
      if (message.messageType) {
        const messageType = typeof message.messageType === 'string' 
          ? message.messageType.toLowerCase() 
          : message.messageType;
        
        return messageType === 'user' || messageType === 'người dùng' || messageType === 'human';
      }
      
      // Kiểm tra nếu có thuộc tính role trong tin nhắn
      if (message.role) {
        const role = typeof message.role === 'string' 
          ? message.role.toLowerCase() 
          : message.role;
        
        return role === 'user' || role === 'human' || role === 'người dùng';
      }
      
      // Kiểm tra nội dung tin nhắn - các tin nhắn chào thường đến từ AI, không phải người dùng
      if (message.content) {
        const content = message.content.toLowerCase();
        // Các tin nhắn chào thường đến từ AI, không phải người dùng
        if (content.includes('xin chào') || 
            content.includes('chào mừng') || 
            content.includes('tôi là trợ lý') || 
            content.includes('tôi có thể giúp')) {
          return false;
        }
        
        // Tin nhắn ngắn, một từ thường là từ người dùng
        if (content.split(' ').length <= 3 && content.length < 20) {
          return true;
        }
      }
      
      // Mặc định là tin nhắn AI nếu không thể xác định
      return false;
    }
  }
};
</script>

<style scoped>
.gemini-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #fafafa;
}

.chat-header {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f9f9f9;
}

.chat-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  max-width: 80%;
  padding: 0;
  border-radius: 12px;
  position: relative;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.user-message {
  align-self: flex-end;
  margin-left: auto;
}

.ai-message {
  align-self: flex-start;
  margin-right: auto;
}

.loading-message {
  opacity: 0.7;
}

.message-content {
  display: flex;
  margin-bottom: 4px;
}

.message-avatar {
  margin-right: 10px;
  margin-top: 4px;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #4285f4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.user-avatar {
  background-color: #5f6368;
}

.message-text {
  background-color: white;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  color: #202124;
  font-size: 15px;
  line-height: 1.5;
  max-width: 100%;
  word-break: break-word;
}

.user-message .message-text {
  background-color: #e8f0fe;
  color: #174ea6;
}

.message-time {
  font-size: 11px;
  color: #5f6368;
  margin-left: 46px;
}

.chat-input {
  display: flex;
  padding: 15px 20px;
  border-top: 1px solid #e0e0e0;
  background-color: white;
  align-items: flex-end;
}

textarea {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #dadce0;
  border-radius: 20px;
  outline: none;
  font-size: 15px;
  font-family: inherit;
  resize: none;
  overflow-y: hidden;
  max-height: 120px;
  line-height: 1.5;
  transition: border-color 0.2s;
}

textarea:focus {
  border-color: #4285f4;
  box-shadow: 0 0 0 3px rgba(66, 133, 244, 0.2);
}

.send-btn {
  margin-left: 10px;
  padding: 0;
  background-color: #4285f4;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-weight: 500;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.send-btn i {
  font-size: 16px;
}

.send-btn:hover {
  background-color: #1a73e8;
}

.send-btn:disabled {
  background-color: #dadce0;
  cursor: not-allowed;
}

/* Typing indicator */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  height: 24px;
}

.typing-indicator span {
  display: inline-block;
  width: 8px;
  height: 8px;
  background-color: #4285f4;
  border-radius: 50%;
  animation: bounce 1.5s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% { 
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% { 
    transform: scale(1.2);
    opacity: 1;
  }
}

/* Message formatting styles */
:deep(ul), :deep(ol) {
  margin: 10px 0;
  padding-left: 24px;
}

:deep(li) {
  margin-bottom: 6px;
}

:deep(strong) {
  font-weight: 600;
}

:deep(em) {
  font-style: italic;
}

:deep(br) {
  line-height: 1.5;
}
</style> 
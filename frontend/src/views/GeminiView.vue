<template>
  <div class="gemini-view">
    <div class="header">
      <h1 class="title">Trợ lý AI</h1>
      <router-link to="/" class="back-button">
        <i class="bi bi-arrow-left"></i> Quay lại
      </router-link>
    </div>

    <div class="main-content">
      <div class="sidebar" v-if="showSidebar">
        <div class="sidebar-header">
          <h3>Lịch sử hội thoại</h3>
          <button class="new-chat-btn" @click="startNewChat">
            <i class="bi bi-plus-lg"></i> Tạo mới
          </button>
        </div>
        <div class="conversation-list">
          <div 
            v-for="convo in conversations" 
            :key="convo.id" 
            class="conversation-item"
            :class="{ active: selectedConversationId === convo.id }"
            @click="loadConversation(convo.id)"
          >
            <div class="conversation-info">
              <div class="conversation-title">{{ convo.firstMessage || 'Hội thoại mới' }}</div>
              <div class="conversation-meta">
                <span class="message-count">{{ convo.messageCount }} tin nhắn</span>
                <span class="date">{{ formatDate(convo.createdAt) }}</span>
              </div>
            </div>
            <button class="delete-btn" @click.stop="deleteConversation(convo.id)">
              <i class="bi bi-trash"></i>
            </button>
          </div>
          <div v-if="conversations.length === 0" class="no-conversations">
            Chưa có hội thoại nào
          </div>
        </div>
      </div>

      <div class="chat-container">
        <div class="chat-header-controls">
          <button class="toggle-sidebar-btn" @click="toggleSidebar">
            <i :class="showSidebar ? 'bi bi-chevron-left' : 'bi bi-clock-history'"></i>
          </button>
        </div>

        <div class="chat-area">
          <GeminiChat
            chatType="general"
            :conversationId="selectedConversationId"
            @conversation-loaded="onConversationLoaded"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import GeminiChat from '@/components/gemini/GeminiChat.vue';
import geminiService from '@/api/geminiService';
import { format } from 'date-fns';

export default {
  name: 'GeminiView',
  components: {
    GeminiChat
  },
  data() {
    return {
      conversations: [],
      showSidebar: true,
      selectedConversationId: null,
      loading: false
    };
  },
  created() {
    this.fetchConversations();
  },
  methods: {
    toggleSidebar() {
      this.showSidebar = !this.showSidebar;
    },
    
    async fetchConversations() {
      try {
        this.loading = true;
        const response = await geminiService.getConversations();
        this.conversations = response.data;
      } catch (error) {
        console.error('Lỗi khi tải hội thoại:', error);
      } finally {
        this.loading = false;
      }
    },
    
    async loadConversation(id) {
      this.selectedConversationId = id;
    },
    
    async deleteConversation(id) {
      if (!confirm('Bạn có chắc chắn muốn xoá hội thoại này không?')) {
        return;
      }
      
      try {
        await geminiService.deleteConversation(id);
        this.conversations = this.conversations.filter(c => c.id !== id);
        
        // If viewing the deleted conversation, reset
        if (this.selectedConversationId === id) {
          this.selectedConversationId = null;
        }
      } catch (error) {
        console.error('Lỗi khi xoá hội thoại:', error);
      }
    },
    
    startNewChat() {
      this.selectedConversationId = null;
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return format(date, 'dd/MM/yyyy HH:mm');
    },
    
    onConversationLoaded(conversationId) {
      console.log('Conversation loaded with ID:', conversationId);
      
      if (conversationId && !this.selectedConversationId) {
        this.selectedConversationId = conversationId;
        this.fetchConversations(); // Update the list
      }
    }
  }
};
</script>

<style scoped>
.gemini-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  color: var(--primary-color);
  margin: 0;
}

.back-button {
  display: flex;
  align-items: center;
  background-color: #f0f0f0;
  color: #333;
  border: none;
  border-radius: 5px;
  padding: 8px 15px;
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.2s;
}

.back-button i {
  margin-right: 8px;
}

.back-button:hover {
  background-color: #e0e0e0;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  height: calc(100vh - 120px);
}

.sidebar {
  width: 300px;
  background-color: #f8f9fa;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
}

.new-chat-btn {
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 5px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.new-chat-btn:hover {
  background-color: #218838;
}

.new-chat-btn i {
  margin-right: 8px;
  font-size: 12px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.conversation-item:hover {
  background-color: #e9ecef;
}

.conversation-item.active {
  background-color: #e2f2ff;
}

.conversation-info {
  flex: 1;
  overflow: hidden;
}

.conversation-title {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}

.conversation-meta {
  display: flex;
  font-size: 12px;
  color: #6c757d;
  margin-top: 4px;
}

.message-count {
  margin-right: 10px;
}

.delete-btn {
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  opacity: 0.7;
  padding: 5px;
}

.delete-btn:hover {
  opacity: 1;
}

.no-conversations {
  padding: 20px;
  text-align: center;
  color: #6c757d;
  font-style: italic;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: white;
}

.chat-header-controls {
  display: flex;
  justify-content: flex-end;
  padding: 10px 15px;
  border-bottom: 1px solid #e0e0e0;
}

.toggle-sidebar-btn {
  background: none;
  border: none;
  color: #6c757d;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
}

.toggle-sidebar-btn:hover {
  background-color: #f0f0f0;
}

.chat-area {
  flex: 1;
  overflow: hidden;
}
</style> 
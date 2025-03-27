<template>
  <div class="gemini-view">
    <div class="header">
      <h1 class="title">Gemini AI</h1>
      <router-link to="/" class="back-button">
        <i class="fas fa-arrow-left"></i> Back
      </router-link>
    </div>

    <div class="main-content">
      <div class="sidebar" v-if="showSidebar">
        <div class="sidebar-header">
          <h3>Chat History</h3>
          <button class="new-chat-btn" @click="startNewChat">
            <i class="fas fa-plus"></i> New Chat
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
              <div class="conversation-title">{{ convo.firstMessage || 'New conversation' }}</div>
              <div class="conversation-meta">
                <span class="message-count">{{ convo.messageCount }} messages</span>
                <span class="date">{{ formatDate(convo.createdAt) }}</span>
              </div>
            </div>
            <button class="delete-btn" @click.stop="deleteConversation(convo.id)">
              <i class="fas fa-trash"></i>
            </button>
          </div>
          <div v-if="conversations.length === 0" class="no-conversations">
            No conversations yet
          </div>
        </div>
      </div>

      <div class="chat-container">
        <div class="tab-buttons">
          <button
            class="tab-button"
            :class="{ active: activeTab === 'general' }"
            @click="activeTab = 'general'"
          >
            General Q&A
          </button>
          <button
            class="tab-button"
            :class="{ active: activeTab === 'revenue' }"
            @click="activeTab = 'revenue'"
          >
            Revenue Prediction
          </button>
          <button class="toggle-sidebar-btn" @click="toggleSidebar">
            <i :class="showSidebar ? 'fas fa-chevron-left' : 'fas fa-history'"></i>
          </button>
        </div>

        <div class="chat-area">
          <GeminiChat
            v-if="activeTab === 'general'"
            chatType="general"
            :conversationId="selectedConversationId"
            @conversation-loaded="onConversationLoaded"
          />
          <GeminiChat
            v-if="activeTab === 'revenue'"
            chatType="revenue"
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
      activeTab: 'general',
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
        console.error('Error fetching conversations:', error);
      } finally {
        this.loading = false;
      }
    },
    
    async loadConversation(id) {
      this.selectedConversationId = id;
    },
    
    async deleteConversation(id) {
      if (!confirm('Are you sure you want to delete this conversation?')) {
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
        console.error('Error deleting conversation:', error);
      }
    },
    
    startNewChat() {
      this.selectedConversationId = null;
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return format(date, 'MM/dd/yyyy HH:mm');
    },
    
    onConversationLoaded(conversationId) {
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
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 5px;
  padding: 8px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-chat-btn i {
  margin-right: 8px;
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

.tab-buttons {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 15px;
  position: relative;
}

.tab-button {
  padding: 15px 20px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-weight: 500;
  color: #6c757d;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.toggle-sidebar-btn {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
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
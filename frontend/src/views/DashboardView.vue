<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>TechFinance</h1>
      <div class="user-menu">
        <span>{{ authStore.user?.fullName || 'Người dùng' }}</span>
        <button @click="logout" class="logout-button">Đăng xuất</button>
      </div>
    </header>
    
    <main class="dashboard-content">
      <div class="welcome-box">
        <h2>Chào mừng đến với Hệ thống Quản lý Thu chi</h2>
        <p>Bạn đã đăng nhập thành công vào hệ thống. Vui lòng chọn chức năng từ menu bên trái để bắt đầu.</p>
      </div>
      
      <!-- Đây sẽ là nơi hiển thị nội dung chính của dashboard -->
    </main>
  </div>
</template>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background-color: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.dashboard-header h1 {
  color: #4f46e5;
  margin: 0;
  font-size: 1.5rem;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.logout-button {
  padding: 0.5rem 1rem;
  background-color: transparent;
  color: #4f46e5;
  border: 1px solid #4f46e5;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.logout-button:hover {
  background-color: #4f46e5;
  color: white;
}

.dashboard-content {
  flex: 1;
  padding: 2rem;
  background-color: #f5f5f5;
}

.welcome-box {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.welcome-box h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 1rem;
}

.welcome-box p {
  color: #6b7280;
  line-height: 1.5;
}
</style> 
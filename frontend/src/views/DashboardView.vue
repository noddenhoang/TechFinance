<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import AppLayout from '../components/layouts/AppLayout.vue'

const authStore = useAuthStore()
const router = useRouter()

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <AppLayout>
    <template #page-title>Dashboard</template>
    
    <div class="welcome-box">
      <h2>Chào mừng đến với Hệ thống Quản lý Thu chi</h2>
      <p>Bạn đã đăng nhập thành công vào hệ thống. Vui lòng chọn chức năng từ menu bên trái để bắt đầu.</p>
    </div>
    
    <!-- Đây sẽ là nơi hiển thị nội dung chính của dashboard -->
  </AppLayout>
</template>

<style scoped>
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
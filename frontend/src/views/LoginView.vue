<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import axios from 'axios'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)
const showPassword = ref(false)
const serverStatus = ref('checking') // 'checking', 'online', 'offline'
const loginHelp = ref(null)
const showLoginHelp = ref(false)

// Check server status and existing auth on component mount
onMounted(async () => {
  serverStatus.value = 'checking'
  const isConnected = await authStore.checkApiStatus()
  serverStatus.value = isConnected ? 'online' : 'offline'
  
  // Try to auto-login with existing tokens
  if (authStore.token && authStore.refreshToken) {
    isLoading.value = true
    try {
      // Check and refresh token if needed
      const isValid = await authStore.checkAndRefreshToken()
      if (isValid) {
        // Verify that the user details are still valid
        await authStore.getCurrentUser()
        router.push('/dashboard')
        return
      }
    } catch (error) {
      console.error('Auto-login failed:', error)
      // If auto-login fails, continue with normal login flow
    } finally {
      isLoading.value = false
    }
  }
  
  // Fetch login help info
  try {
    const response = await axios.get(`${axios.defaults.baseURL}/api/auth/help`)
    loginHelp.value = response.data
  } catch (error) {
    console.error('Could not get login help:', error)
  }
  
  // Create default users if needed
  try {
    await axios.post(`${axios.defaults.baseURL}/api/public/init/create-admin`)
    console.log('User initialization checked')
  } catch (error) {
    console.error('User initialization error:', error)
  }
})

const login = async () => {
  if (!username.value || !password.value) {
    errorMessage.value = 'Vui lòng nhập tên đăng nhập và mật khẩu'
    return
  }
  
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    const success = await authStore.login(username.value, password.value)
    if (success) {
      router.push('/dashboard')
    } else {
      errorMessage.value = authStore.error || 'Đăng nhập thất bại'
    }
  } catch (error) {
    console.error('Login error:', error)
    
    // Check if the error is related to CORS or connection issues
    if (error.message && error.message.includes('Network Error')) {
      errorMessage.value = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng hoặc máy chủ có đang chạy không.'
      serverStatus.value = 'offline'
    } else if (error.response) {
      if (error.response.status === 403) {
        errorMessage.value = 'Máy chủ từ chối truy cập. Vui lòng kiểm tra lại thông tin đăng nhập hoặc liên hệ quản trị viên.'
      } else if (error.response.status === 500) {
        errorMessage.value = 'Lỗi máy chủ nội bộ. Vui lòng thử lại sau hoặc liên hệ quản trị viên.'
      } else {
        errorMessage.value = error.response.data?.error || 'Có lỗi xảy ra, vui lòng thử lại sau'
      }
    } else {
      errorMessage.value = 'Có lỗi xảy ra, vui lòng thử lại sau'
    }
  } finally {
    isLoading.value = false
  }
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const getStatusColor = () => {
  if (serverStatus.value === 'online') return 'text-green-600'
  if (serverStatus.value === 'offline') return 'text-red-600'
  return 'text-yellow-600'
}

const getStatusText = () => {
  if (serverStatus.value === 'online') return 'Máy chủ hoạt động'
  if (serverStatus.value === 'offline') return 'Máy chủ không phản hồi'
  return 'Đang kiểm tra...'
}

const toggleLoginHelp = () => {
  showLoginHelp.value = !showLoginHelp.value
}

const useTestAccount = (type) => {
  if (type === 'admin') {
    username.value = 'admin'
    password.value = 'admin123'
  } else {
    username.value = 'user'
    password.value = 'user123'
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">TechFinance</h1>
      <h2 class="login-subtitle">Hệ thống Quản lý Thu chi</h2>
      
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
      
      <form @submit.prevent="login" class="login-form">
        <div class="form-group">
          <label for="username">Tên đăng nhập</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="Nhập tên đăng nhập"
            autocomplete="username"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">Mật khẩu</label>
          <div class="password-input-container">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Nhập mật khẩu"
              autocomplete="current-password"
              required
            />
            <button 
              type="button" 
              class="toggle-password-button"
              @click="togglePasswordVisibility"
              aria-label="Toggle password visibility"
            >
              {{ showPassword ? 'Ẩn' : 'Hiện' }}
            </button>
          </div>
        </div>
        
        <button type="submit" class="login-button" :disabled="isLoading || serverStatus === 'offline'">
          {{ isLoading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
        </button>
      </form>
      
      <div v-if="serverStatus === 'offline'" class="help-text">
        <p>Không thể kết nối đến máy chủ. Vui lòng kiểm tra:</p>
        <ul>
          <li>Máy chủ đang chạy</li>
          <li>Kết nối mạng của bạn</li>
          <li>Cổng máy chủ đã được mở (mặc định: 8080)</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  color: #4f46e5;
  margin-bottom: 0.5rem;
  font-size: 1.75rem;
}

.login-subtitle {
  text-align: center;
  color: #6b7280;
  margin-bottom: 1rem;
  font-size: 1rem;
  font-weight: normal;
}

.server-status-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  margin-bottom: 1rem;
  border-radius: 4px;
  background-color: #f9fafb;
  font-size: 0.8rem;
}

.refresh-button {
  background: none;
  border: none;
  color: #4f46e5;
  font-size: 0.8rem;
  cursor: pointer;
  text-decoration: underline;
}

.text-green-600 {
  color: #059669;
}

.text-red-600 {
  color: #dc2626;
}

.text-yellow-600 {
  color: #d97706;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.form-group input {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 1rem;
  transition: border-color 0.15s ease-in-out;
}

.form-group input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.login-button {
  padding: 0.75rem 1rem;
  background-color: #4f46e5;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease-in-out;
}

.login-button:hover {
  background-color: #4338ca;
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  background-color: #fee2e2;
  color: #b91c1c;
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 1rem;
  font-size: 0.875rem;
}

.password-input-container {
  position: relative;
  display: flex;
}

.password-input-container input {
  flex: 1;
  padding-right: 4.5rem; /* Make space for the button */
}

.toggle-password-button {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #4f46e5;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
  padding: 0.25rem 0.5rem;
}

.toggle-password-button:hover {
  color: #4338ca;
}

.login-help-section {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.login-help-toggle {
  background: none;
  border: none;
  color: #4f46e5;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 0;
  display: block;
  margin: 0 auto;
  text-decoration: underline;
}

.login-help-content {
  margin-top: 1rem;
  background-color: #f9fafb;
  border-radius: 0.375rem;
  padding: 1rem;
}

.test-account {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  border-radius: 0.25rem;
  background-color: white;
  margin-bottom: 0.75rem;
}

.account-info {
  font-size: 0.875rem;
}

.use-account-button {
  background-color: #4f46e5;
  color: white;
  border: none;
  border-radius: 0.25rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  cursor: pointer;
}

.help-note {
  font-size: 0.75rem;
  color: #6b7280;
  text-align: center;
  margin-top: 0.5rem;
}

.help-text {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  color: #4b5563;
}

.help-text p {
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.help-text ul {
  padding-left: 1.5rem;
  margin: 0;
}

.help-text li {
  margin-bottom: 0.25rem;
}
</style>
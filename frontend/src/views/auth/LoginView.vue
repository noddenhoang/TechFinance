<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

const username = ref('');
const password = ref('');
const rememberMe = ref(false);
const showPassword = ref(false);
const isLoggingIn = ref(false);
const loginError = ref('');
const errors = reactive({
  username: '',
  password: ''
});

const router = useRouter();
const authStore = useAuthStore();

// Toggle password visibility
function togglePasswordVisibility() {
  showPassword.value = !showPassword.value;
}

// Validate form
function validateForm() {
  let isValid = true;
  
  // Reset errors
  errors.username = '';
  errors.password = '';
  
  if (!username.value.trim()) {
    errors.username = 'Vui lòng nhập tên đăng nhập';
    isValid = false;
  }
  
  if (!password.value) {
    errors.password = 'Vui lòng nhập mật khẩu';
    isValid = false;
  }
  
  return isValid;
}

// Handle login
async function login() {
  // Clear any previous errors
  loginError.value = '';
  
  // Validate form
  if (!validateForm()) {
    return;
  }
  
  isLoggingIn.value = true;
  
  try {
    // Call the login action from the auth store
    await authStore.login(username.value, password.value);
    
    // If login is successful, redirect to dashboard
    router.push({ name: 'dashboard' });
  } catch (error) {
    console.error('Login failed:', error);
    
    // Handle different types of errors
    if (error.response && error.response.status === 401) {
      loginError.value = 'Tên đăng nhập hoặc mật khẩu không chính xác';
    } else if (error.message === 'Network Error') {
      loginError.value = 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng';
    } else {
      loginError.value = 'Đã xảy ra lỗi. Vui lòng thử lại sau';
    }
  } finally {
    isLoggingIn.value = false;
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">TechFinance</div>
        <h2 class="title">Đăng nhập</h2>
        <p class="subtitle">Đăng nhập để quản lý tài chính của bạn</p>
      </div>
      
      <form @submit.prevent="login" class="login-form">
        <div class="form-group">
          <label class="form-label">Tên đăng nhập</label>
          <div class="input-icon-wrapper">
            <i class="input-icon-left bi bi-person"></i>
            <input 
              v-model="username" 
              type="text" 
              class="form-input has-icon" 
              :class="{'error': errors.username}"
              placeholder="Nhập tên đăng nhập"
              autocomplete="username"
            >
          </div>
          <div v-if="errors.username" class="form-error">{{ errors.username }}</div>
        </div>
        
        <div class="form-group">
          <label class="form-label">Mật khẩu</label>
          <div class="input-icon-wrapper">
            <i class="input-icon-left bi bi-lock"></i>
            <input 
              v-model="password" 
              :type="showPassword ? 'text' : 'password'" 
              class="form-input has-icon" 
              :class="{'error': errors.password}"
              placeholder="Nhập mật khẩu"
              autocomplete="current-password"
            >
            <button 
              type="button"
              class="input-icon-right clickable"
              @click="togglePasswordVisibility"
            >
              <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
          <div v-if="errors.password" class="form-error">{{ errors.password }}</div>
        </div>
        
        <div class="form-options">
          <div class="checkbox-wrapper">
            <input type="checkbox" id="remember" v-model="rememberMe">
            <label for="remember">Ghi nhớ đăng nhập</label>
          </div>
          <a href="#" class="forgot-password">Quên mật khẩu?</a>
        </div>
        
        <div v-if="loginError" class="login-error">
          <i class="bi bi-exclamation-triangle"></i> {{ loginError }}
        </div>
        
        <button type="submit" class="btn-login" :disabled="isLoggingIn">
          <i v-if="isLoggingIn" class="bi bi-arrow-repeat spinner"></i>
          <span>{{ isLoggingIn ? 'Đang đăng nhập...' : 'Đăng nhập' }}</span>
        </button>
      </form>
      
      <div class="login-footer">
        <p>&copy; {{ new Date().getFullYear() }} TechFinance. Bản quyền thuộc về TechZen Academy.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f9fafb;
  background-image: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 1rem;
}

.login-box {
  width: 100%;
  max-width: 400px;
  background-color: white;
  border-radius: 0.75rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 2rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  background-color: #f8fafc;
}

.logo {
  font-size: 1.875rem;
  font-weight: 700;
  background: linear-gradient(to right, #4f46e5, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 1rem;
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.subtitle {
  font-size: 0.875rem;
  color: #64748b;
}

.login-form {
  padding: 1.5rem;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
}

.checkbox-wrapper input {
  margin-right: 0.5rem;
  accent-color: #4f46e5;
}

.forgot-password {
  color: #4f46e5;
  font-size: 0.875rem;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-password:hover {
  color: #4338ca;
  text-decoration: underline;
}

.login-error {
  padding: 0.75rem;
  margin-bottom: 1rem;
  background-color: #fee2e2;
  color: #b91c1c;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.btn-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: #4f46e5;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-login:hover {
  background-color: #4338ca;
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 0.75rem;
}

.clickable {
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  color: #6b7280;
}

.clickable:hover {
  color: #4f46e5;
}

/* Animation for spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner {
  animation: spin 1s linear infinite;
}
</style>

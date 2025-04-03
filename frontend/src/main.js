import './assets/main.css'
import 'bootstrap-icons/font/bootstrap-icons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './assets/styles/common.css'
import axios from 'axios'
import { useAuthStore } from './stores/auth'

// Set base URL for API requests
axios.defaults.baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Setup axios interceptor for token refresh
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config
    
    // If error is 401 (Unauthorized) and not already retrying
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      const authStore = useAuthStore()
      
      // Try to refresh the token
      if (await authStore.refreshToken()) {
        // Update the token in the original request
        originalRequest.headers['Authorization'] = `Bearer ${authStore.token}`
        // Retry the original request
        return axios(originalRequest)
      }
      
      // If refresh failed, redirect to login
      router.push('/login')
    }
    
    return Promise.reject(error)
  }
)

// Restore existing token from localStorage if available
const authStore = useAuthStore()
if (authStore.token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${authStore.token}`
}

app.mount('#app')

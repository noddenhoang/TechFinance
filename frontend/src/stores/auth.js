import { defineStore } from 'pinia'
import axios from 'axios'
import api from '../services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    loading: false,
    refreshing: false,
    error: null,
    apiStatus: 'unknown', // 'unknown', 'connected', 'error'
    lastRefresh: parseInt(localStorage.getItem('lastRefresh')) || 0
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    getUser: (state) => state.user,
    getApiStatus: (state) => state.apiStatus,
    needsRefresh: (state) => {
      // Check if token should be refreshed (every 50 minutes)
      const now = Date.now();
      const refreshInterval = 50 * 60 * 1000; // 50 minutes in milliseconds
      return state.token && state.refreshToken && (now - state.lastRefresh > refreshInterval);
    }
  },
  
  actions: {
    async checkApiStatus() {
      try {
        // Use OPTIONS request which is less likely to cause server errors
        await axios.options(`${axios.defaults.baseURL}/api/auth/login`, {
          timeout: 3000 // 3 seconds timeout
        })
        this.apiStatus = 'connected'
        return true
      } catch (error) {
        console.log('API status check error:', error)
        
        // If we get any response, the API is available
        if (error.response) {
          this.apiStatus = 'connected'
          return true
        }
        this.apiStatus = 'error'
        return false
      }
    },
    
    async login(username, password) {
      this.loading = true;
      this.error = null;
      
      try {
        // First check if API is available
        await this.checkApiStatus();
        
        if (this.apiStatus !== 'connected') {
          this.error = 'Không thể kết nối đến máy chủ';
          return false;
        }
        
        const response = await api.auth.login(username, password);
        console.log('Login response:', response.data);
        
        // Extract tokens and user details from response
        const { token, refreshToken, user } = response.data;
        
        if (!token) {
          this.error = 'Không nhận được token từ máy chủ';
          return false;
        }
        
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
        this.lastRefresh = Date.now();
        
        // Store in localStorage for persistence
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('lastRefresh', this.lastRefresh.toString());
        
        // Set Authorization header for future requests
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        return true;
      } catch (error) {
        console.error('Login error details:', error)
        
        if (error.response) {
          // The request was made and the server responded with a status code
          // that falls out of the range of 2xx
          switch (error.response.status) {
            case 401:
              this.error = 'Tên đăng nhập hoặc mật khẩu không đúng'
              break
            case 403:
              this.error = 'Tài khoản của bạn không có quyền truy cập'
              break
            case 500:
              this.error = 'Lỗi máy chủ, vui lòng thử lại sau'
              break
            default:
              this.error = error.response.data?.error || 'Đăng nhập thất bại'
          }
        } else if (error.request) {
          // The request was made but no response was received
          this.error = 'Không thể kết nối đến máy chủ, vui lòng kiểm tra kết nối mạng'
        } else {
          // Something happened in setting up the request that triggered an Error
          this.error = 'Lỗi không xác định: ' + error.message
        }
        return false
      } finally {
        this.loading = false
      }
    },
    
    async refreshToken() {
      if (!this.refreshToken) return false;
      
      this.refreshing = true;
      
      try {
        const response = await axios.post(`${axios.defaults.baseURL}/api/auth/refresh-token`, {
          refreshToken: this.refreshToken
        });
        
        const { token, refreshToken, user } = response.data;
        
        this.token = token;
        if (refreshToken) this.refreshToken = refreshToken; // Update if a new refresh token was provided
        if (user) this.user = user;
        this.lastRefresh = Date.now();
        
        // Update localStorage
        localStorage.setItem('token', token);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
        if (user) localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('lastRefresh', this.lastRefresh.toString());
        
        // Update Authorization header
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        return true;
      } catch (error) {
        console.error('Token refresh failed:', error);
        
        // If refresh fails due to invalid refresh token, log the user out
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
          this.logout();
        }
        
        return false;
      } finally {
        this.refreshing = false;
      }
    },
    
    async checkAndRefreshToken() {
      if (this.needsRefresh) {
        return await this.refreshToken();
      }
      return !!this.token; // Return true if token exists
    },
    
    async getCurrentUser() {
      if (!this.token) return null
      
      try {
        const response = await api.auth.getCurrentUser()
        this.user = response.data
        localStorage.setItem('user', JSON.stringify(this.user))
        return this.user
      } catch (error) {
        // If 401 Unauthorized, logout
        if (error.response?.status === 401) {
          this.logout()
        }
        return null
      }
    },
    
    logout() {
      // Optional: Call logout API
      if (this.token) {
        api.auth.logout().catch((error) => {
          // Just log errors on logout
          console.error('Logout error:', error)
        })
      }
      
      this.token = null
      this.refreshToken = null;
      this.user = null
      this.lastRefresh = 0;
      
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('lastRefresh');
      delete axios.defaults.headers.common['Authorization'];
    }
  }
});
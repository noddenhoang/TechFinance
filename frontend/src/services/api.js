import axios from 'axios'

const api = {
  // Auth API calls
  auth: {
    login(username, password) {
      return axios.post('/api/auth/login', { username, password })
    },
    logout() {
      return axios.post('/api/auth/logout')
    },
    getCurrentUser() {
      return axios.get('/api/auth/me')
    },
    register(userData) {
      return axios.post('/api/auth/register', userData)
    },
    refreshToken(refreshToken) {
      return axios.post('/api/auth/refresh-token', { refreshToken })
    }
  },
  
  // You can add more API modules here as needed:
  // Income transactions API
  income: {
    getAll(params) {
      return axios.get('/api/income/transactions', { params })
    },
    getById(id) {
      return axios.get(`/api/income/transactions/${id}`)
    },
    create(data) {
      return axios.post('/api/income/transactions', data)
    },
    update(id, data) {
      return axios.put(`/api/income/transactions/${id}`, data)
    },
    delete(id) {
      return axios.delete(`/api/income/transactions/${id}`)
    }
  },
  
  // Expense transactions API
  expense: {
    getAll(params) {
      return axios.get('/api/expense/transactions', { params })
    },
    getById(id) {
      return axios.get(`/api/expense/transactions/${id}`)
    },
    create(data) {
      return axios.post('/api/expense/transactions', data)
    },
    update(id, data) {
      return axios.put(`/api/expense/transactions/${id}`, data)
    },
    delete(id) {
      return axios.delete(`/api/expense/transactions/${id}`)
    }
  },
  
  // Set up interceptors to handle common errors
  setupInterceptors() {
    // Response interceptor
    axios.interceptors.response.use(
      (response) => response,
      (error) => {
        // Handle session expiration
        if (error.response && error.response.status === 401) {
          // Clear auth data
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          
          // Redirect to login if not already there
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
        }
        return Promise.reject(error)
      }
    )
  }
}

// Initialize interceptors
api.setupInterceptors()

export default api
import axios from 'axios'

const BASE_URL = '/api/income-categories'

export const incomeCategories = {
  // Get all income categories with filters
  getAll: async (filters = {}) => {
    try {
      const params = new URLSearchParams()
      
      // Only add parameters with values
      if (filters.name) {
        params.append('name', filters.name)
      }
      
      // Special handling for isActive (only send when true or false, not null)
      if (filters.isActive === true || filters.isActive === false) {
        params.append('isActive', filters.isActive)
      }
      
      // Format date according to ISO 8601
      if (filters.createdAt) {
        const date = new Date(filters.createdAt)
        if (!isNaN(date.getTime())) {
          params.append('createdAt', date.toISOString().split('T')[0])
        }
      }

      const token = localStorage.getItem('token')
      const response = await axios.get(`${BASE_URL}${params.toString() ? '?' + params.toString() : ''}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching categories:', error)
      throw error
    }
  },

  // Get active income categories only
  getActive: async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await axios.get(`${BASE_URL}/active`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching active categories:', error)
      throw error
    }
  },

  // Get income category by ID
  getById: async (id) => {
    try {
      const token = localStorage.getItem('token')
      const response = await axios.get(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error(`Error fetching category ${id}:`, error)
      throw error
    }
  },

  // Create new income category
  create: async (data) => {
    try {
      const token = localStorage.getItem('token')
      const response = await axios.post(BASE_URL, {
        name: data.name,
        description: data.description,
        isActive: data.isActive
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error('Error creating category:', error)
      throw error
    }
  },

  // Update income category
  update: async (id, data) => {
    try {
      const token = localStorage.getItem('token')
      const response = await axios.put(`${BASE_URL}/${id}`, {
        name: data.name,
        description: data.description,
        isActive: data.isActive
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error) {
      console.error(`Error updating category ${id}:`, error)
      throw error
    }
  },

  // Delete income category
  delete: async (id) => {
    try {
      const token = localStorage.getItem('token')
      await axios.delete(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
    } catch (error) {
      console.error(`Error deleting category ${id}:`, error)
      throw error
    }
  }
}
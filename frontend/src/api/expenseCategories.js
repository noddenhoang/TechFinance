import axios from 'axios'

const BASE_URL = '/api/expense-categories'

export const expenseCategories = {
  async getAll(filters = {}, page = 0, size = 8, sortField = 'id', sortDirection = 'asc') {
    try {
      const params = new URLSearchParams();
      
      // Thêm các filter
      if (filters.name) {
        params.append('name', filters.name);
      }
      
      if (filters.isActive !== null && filters.isActive !== undefined) {
        params.append('isActive', filters.isActive);
      }
      
      // Thêm tham số phân trang
      params.append('page', page.toString());
      params.append('size', size.toString());
      params.append('sort', sortField + ',' + sortDirection);
      
      const queryString = params.toString() ? `?${params.toString()}` : '';
      const token = localStorage.getItem('token');
      
      // Sửa trực tiếp endpoint ở đây, xóa '/paged'
      const response = await axios.get(`${BASE_URL}${queryString}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expense categories:', error);
      throw error;
    }
  },

  // Get expense category by ID
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

  // Create new expense category
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

  // Update expense category
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

  // Delete expense category
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
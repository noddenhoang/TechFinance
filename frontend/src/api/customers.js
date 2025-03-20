import axios from 'axios'

const BASE_URL = '/api/customers'

export const customers = {
  // Get all customers with pagination
  async getAll(filters = {}, page = 0, size = 8, sortField = 'id', sortDirection = 'asc') {
    try {
      const params = new URLSearchParams();
      
      // Add filters
      if (filters.name) {
        params.append('name', filters.name);
      }
      
      // Thêm filter isActive nếu được cung cấp
      if (filters.isActive !== undefined && filters.isActive !== null) {
        params.append('isActive', filters.isActive);
      }
      
      // Add pagination parameters
      params.append('page', page.toString());
      params.append('size', size.toString());
      params.append('sort', `${sortField},${sortDirection}`);
      
      const queryString = params.toString() ? `?${params.toString()}` : '';
      const token = localStorage.getItem('token');
      
      const response = await axios.get(`${BASE_URL}${queryString}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching customers:', error);
      throw error;
    }
  },
  
  // Get customer by ID
  async getById(id) {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error(`Error fetching customer ${id}:`, error);
      throw error;
    }
  },
  
  // Create new customer
  async create(data) {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(BASE_URL, data, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error creating customer:', error);
      throw error;
    }
  },
  
  // Update existing customer
  async update(id, data) {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.put(`${BASE_URL}/${id}`, data, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error(`Error updating customer ${id}:`, error);
      throw error;
    }
  },
  
  // Delete customer
  async delete(id) {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    } catch (error) {
      console.error(`Error deleting customer ${id}:`, error);
      throw error;
    }
  }
}
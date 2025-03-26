import axios from 'axios'

const BASE_URL = '/api/suppliers'

export const suppliers = {
  // Get all suppliers with pagination
  async getAll(filters = {}, page = 0, size = 8, sortField = 'id', sortDirection = 'asc') {
    try {
      const params = new URLSearchParams();
      
      // Add filters
      if (filters.name) {
        params.append('name', filters.name);
      }
      
      // Add additional filter parameters
      if (filters.email) {
        params.append('email', filters.email);
      }
      
      if (filters.phone) {
        params.append('phone', filters.phone);
      }
      
      if (filters.address) {
        params.append('address', filters.address);
      }
      
      if (filters.taxCode) {
        params.append('taxCode', filters.taxCode);
      }
      
      // Thêm filter isActive nếu được cung cấp
      if (filters.isActive !== undefined && filters.isActive !== null) {
        params.append('isActive', filters.isActive);
      }
      
      // Add pagination parameters
      params.append('page', page.toString());
      params.append('size', size.toString());
      params.append('sort', `${sortField},${sortDirection}`);
      
      // ĐÂY LÀ PHẦN THIẾU - XÂY DỰNG URL VÀ THỰC HIỆN REQUEST
      const queryString = params.toString() ? `?${params.toString()}` : '';
      const token = localStorage.getItem('token');
      
      const response = await axios.get(`${BASE_URL}${queryString}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      console.error('Error fetching suppliers:', error);
      throw error;
    }
  },
  
  // Get supplier by ID
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
      console.error(`Error fetching supplier ${id}:`, error);
      throw error;
    }
  },
  
  // Create new supplier
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
      console.error('Error creating supplier:', error);
      throw error;
    }
  },
  
  // Update existing supplier
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
      console.error(`Error updating supplier ${id}:`, error);
      
      if (error.response?.status === 404) {
        throw new Error("Nhà cung cấp không tồn tại hoặc đã bị xóa");
      }
      throw error;
    }
  },
  
  // Delete supplier
  async delete(id) {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    } catch (error) {
      console.error(`Error deleting supplier ${id}:`, error);
      
      if (error.response?.status === 404) {
        throw new Error("Nhà cung cấp không tồn tại hoặc đã bị xóa");
      }
      throw error;
    }
  }
}
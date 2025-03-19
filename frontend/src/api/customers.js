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
  }
}
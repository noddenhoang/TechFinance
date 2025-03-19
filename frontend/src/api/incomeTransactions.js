import axios from 'axios';

const BASE_URL = '/api/income-transactions';

export const incomeTransactions = {
  // Get all income transactions with pagination and filtering
  async getAll(filters = {}, page = 0, size = 10, sortField = 'transactionDate', sortDirection = 'desc') {
    try {
      const params = new URLSearchParams();
      
      // Add filters
      if (filters.customerId) {
        params.append('customerId', filters.customerId);
      }
      
      if (filters.categoryId) {
        params.append('categoryId', filters.categoryId);
      }
      
      if (filters.startDate) {
        params.append('startDate', filters.startDate);
      }
      
      if (filters.endDate) {
        params.append('endDate', filters.endDate);
      }
      
      if (filters.minAmount) {
        params.append('minAmount', filters.minAmount);
      }
      
      if (filters.maxAmount) {
        params.append('maxAmount', filters.maxAmount);
      }
      
      if (filters.paymentStatus) {
        params.append('paymentStatus', filters.paymentStatus);
      }
      
      if (filters.referenceNo) {
        params.append('referenceNo', filters.referenceNo);
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
      console.error('Error fetching income transactions:', error);
      throw error;
    }
  },
  
  // Get transaction by ID
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
      console.error(`Error fetching income transaction ${id}:`, error);
      throw error;
    }
  },
  
  // Create new transaction
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
      console.error('Error creating transaction:', error);
      throw error;
    }
  },
  
  // Update transaction
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
      console.error(`Error updating transaction ${id}:`, error);
      throw error;
    }
  },
  
  // Delete transaction
  async delete(id) {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    } catch (error) {
      console.error(`Error deleting transaction ${id}:`, error);
      throw error;
    }
  }
  
  // Đã xóa phương thức exportData
};
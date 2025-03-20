import axios from 'axios';

const BASE_URL = '/api/expense-transactions';

export const expenseTransactions = {
  // Get all expense transactions with pagination and filtering
  async getAll(filters = {}, page = 0, size = 10, sortField = 'transactionDate', sortDirection = 'desc') {
    try {
      const params = new URLSearchParams();
      
      // Add filters
      if (filters.supplierId) {
        params.append('supplierId', filters.supplierId);
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
      
      // Add pagination
      params.append('page', page);
      params.append('size', size);
      
      // Add sorting
      params.append('sort', `${sortField},${sortDirection}`);
      
      // Get the token from localStorage
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      const queryString = params.toString() ? `?${params.toString()}` : '';
      
      const response = await axios.get(`${BASE_URL}${queryString}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      console.error('Error fetching expense transactions:', error);
      throw error;
    }
  },
  
  // Get transaction by ID
  async getById(id) {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      const response = await axios.get(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      console.error(`Error fetching expense transaction with ID ${id}:`, error);
      throw error;
    }
  },
  
  // Create new transaction
  async create(data) {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      const response = await axios.post(BASE_URL, data, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      return response.data;
    } catch (error) {
      console.error('Error creating expense transaction:', error);
      throw error;
    }
  },
  
  // Update transaction
  async update(id, data) {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      const response = await axios.put(`${BASE_URL}/${id}`, data, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      return response.data;
    } catch (error) {
      console.error(`Error updating expense transaction with ID ${id}:`, error);
      throw error;
    }
  },
  
  // Delete transaction
  async delete(id) {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      await axios.delete(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return true;
    } catch (error) {
      console.error(`Error deleting expense transaction with ID ${id}:`, error);
      throw error;
    }
  }
};
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const BASE_URL = '/api/budget-overview';

export const budgetOverview = {
  // Lấy tổng quan ngân sách
  async getBudgetOverview(year, month) {
    const params = new URLSearchParams();
    
    if (year) {
      params.append('year', year);
    }
    
    if (month !== null && month !== undefined) {
      params.append('month', month);
    }
    
    const queryString = params.toString() ? `?${params.toString()}` : '';
    
    try {
      const auth = useAuthStore();
      const response = await axios.get(`${BASE_URL}${queryString}`, {
        headers: {
          'Authorization': `Bearer ${auth.token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching budget overview:', error);
      throw error;
    }
  }
}; 
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const BASE_URL = '/api/expense-budgets';

export const expenseBudgets = {
  // Lấy danh sách ngân sách chi tiêu có phân trang và bộ lọc
  async getAll(filters = {}, page = 0, size = 10, sortField = 'year', sortDirection = 'desc') {
    const params = new URLSearchParams();
    
    // Thêm các tham số filter
    if (filters.year) {
      params.append('year', filters.year);
    }
    
    // Chỉ thêm month khi là giá trị có ý nghĩa (không phải null, undefined hoặc chuỗi rỗng)
    if (filters.month !== null && filters.month !== undefined && filters.month !== '') {
      params.append('month', filters.month);
    }
    
    if (filters.categoryId) {
      params.append('categoryId', filters.categoryId);
    }
    
    // Thêm tham số phân trang và sắp xếp
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', `${sortField},${sortDirection}`);
    
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
      console.error('Error fetching expense budgets:', error);
      throw error;
    }
  },
  
  // Lấy chi tiết ngân sách theo ID
  async getById(id) {
    try {
      const auth = useAuthStore();
      const response = await axios.get(`${BASE_URL}/${id}`, {
        headers: {
          'Authorization': `Bearer ${auth.token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching expense budget details:', error);
      throw error;
    }
  },
  
  // Cập nhật lại ngân sách tự động theo chi tiêu thực tế
  async refresh(year, month) {
    try {
      const auth = useAuthStore();
      const params = new URLSearchParams();
      
      if (year) {
        params.append('year', year);
      }
      
      if (month) {
        params.append('month', month);
      }
      
      const queryString = params.toString() ? `?${params.toString()}` : '';
      
      await axios.get(`${BASE_URL}/refresh${queryString}`, {
        headers: {
          'Authorization': `Bearer ${auth.token}`
        }
      });
    } catch (error) {
      console.error('Error refreshing expense budgets:', error);
      throw error;
    }
  }
};
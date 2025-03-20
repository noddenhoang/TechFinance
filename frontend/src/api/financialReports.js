import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const BASE_URL = '/api/reports';

export const financialReports = {
  // Lấy báo cáo tài chính theo tháng
  async getMonthlyReport(year, month) {
    const params = new URLSearchParams();
    
    if (year) {
      params.append('year', year);
    }
    
    if (month) {
      params.append('month', month);
    }
    
    const queryString = params.toString() ? `?${params.toString()}` : '';
    
    try {
      const auth = useAuthStore();
      const response = await axios.get(`${BASE_URL}/monthly${queryString}`, {
        headers: {
          'Authorization': `Bearer ${auth.token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching monthly report:', error);
      throw error;
    }
  }
};
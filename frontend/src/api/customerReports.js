import { api } from './api';

export const customerReports = {
  /**
   * Lấy báo cáo khách hàng theo tháng
   * @param {number} year - Năm báo cáo
   * @param {number} month - Tháng báo cáo
   * @returns {Promise<Object>} - Dữ liệu báo cáo
   */
  async getMonthlyReport(year, month) {
    try {
      const params = new URLSearchParams();
      params.append('year', year);
      params.append('month', month);
      const response = await api.get(`/api/reports/customers/monthly?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error getting monthly customer report:', error);
      throw error;
    }
  },

  /**
   * Lấy báo cáo khách hàng theo quý
   * @param {number} year - Năm báo cáo
   * @param {number} quarter - Quý báo cáo (1-4)
   * @returns {Promise<Object>} - Dữ liệu báo cáo
   */
  async getQuarterlyReport(year, quarter) {
    try {
      const params = new URLSearchParams();
      params.append('year', year);
      params.append('quarter', quarter);
      const response = await api.get(`/api/reports/customers/quarterly?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error getting quarterly customer report:', error);
      throw error;
    }
  },

  /**
   * Lấy báo cáo khách hàng theo năm
   * @param {number} year - Năm báo cáo
   * @returns {Promise<Object>} - Dữ liệu báo cáo
   */
  async getYearlyReport(year) {
    try {
      const params = new URLSearchParams();
      params.append('year', year);
      const response = await api.get(`/api/reports/customers/yearly?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error getting yearly customer report:', error);
      throw error;
    }
  }
}; 
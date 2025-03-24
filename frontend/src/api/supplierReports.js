import { api } from './api';

export const supplierReports = {
  /**
   * Lấy báo cáo nhà cung cấp theo tháng
   * @param {number} year - Năm báo cáo
   * @param {number} month - Tháng báo cáo
   * @returns {Promise<Object>} - Dữ liệu báo cáo
   */
  async getMonthlyReport(year, month) {
    try {
      const params = new URLSearchParams();
      params.append('year', year);
      params.append('month', month);
      const response = await api.get(`/api/reports/suppliers/monthly?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error getting monthly supplier report:', error);
      throw error;
    }
  },

  /**
   * Lấy báo cáo nhà cung cấp theo quý
   * @param {number} year - Năm báo cáo
   * @param {number} quarter - Quý báo cáo (1-4)
   * @returns {Promise<Object>} - Dữ liệu báo cáo
   */
  async getQuarterlyReport(year, quarter) {
    try {
      const params = new URLSearchParams();
      params.append('year', year);
      params.append('quarter', quarter);
      const response = await api.get(`/api/reports/suppliers/quarterly?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error getting quarterly supplier report:', error);
      throw error;
    }
  }
}; 
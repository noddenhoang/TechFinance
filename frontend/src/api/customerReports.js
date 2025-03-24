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
      const response = await api.get(`/api/reports/customers/monthly/${year}/${month}`);
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
      const response = await api.get(`/api/reports/customers/quarterly/${year}/${quarter}`);
      return response.data;
    } catch (error) {
      console.error('Error getting quarterly customer report:', error);
      throw error;
    }
  }
}; 
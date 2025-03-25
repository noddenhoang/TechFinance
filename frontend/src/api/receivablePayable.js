import { api } from './api'

/**
 * Service for interacting with the Receivable/Payable Reports API
 */
export const receivablePayable = {
  /**
   * Get receivable and payable report data for charts
   * @param {Object} params - Parameters object
   * @param {Number} [params.year] - Optional year filter
   * @param {Number} [params.month] - Optional month filter
   * @returns {Promise} - Promise with the API response
   */
  getChartData: async (params = {}) => {
    const { year, month } = params
    let url = '/api/reports/receivable-payable'
    
    const queryParams = []
    if (year !== undefined) queryParams.push(`year=${year}`)
    if (month !== undefined) queryParams.push(`month=${month}`)
    
    if (queryParams.length > 0) {
      url += `?${queryParams.join('&')}`
    }
    
    console.log('Making API request to:', url);
    
    try {
      const response = await api.get(url)
      console.log('API response:', response.data);
      return response.data
    } catch (error) {
      console.error('Error fetching receivable/payable chart data:', error);
      if (error.response) {
        console.error('Error response:', error.response.data);
        console.error('Status:', error.response.status);
      } else if (error.request) {
        console.error('No response received:', error.request);
      }
      throw error;
    }
  },
  
  /**
   * Get detailed receivable and payable report data
   * @param {Object} params - Parameters object
   * @param {Number} [params.year] - Optional year filter
   * @param {Number} [params.month] - Optional month filter
   * @returns {Promise} - Promise with the API response
   */
  getDetailedReport: async (params = {}) => {
    const { year, month } = params
    let url = '/api/reports/receivable-payable/detailed'
    
    const queryParams = []
    if (year !== undefined) queryParams.push(`year=${year}`)
    if (month !== undefined) queryParams.push(`month=${month}`)
    
    if (queryParams.length > 0) {
      url += `?${queryParams.join('&')}`
    }
    
    console.log('Making API request to:', url);
    
    try {
      const response = await api.get(url)
      console.log('API response:', response.data);
      return response.data
    } catch (error) {
      console.error('Error fetching detailed receivable/payable report:', error);
      if (error.response) {
        console.error('Error response:', error.response.data);
        console.error('Status:', error.response.status);
      } else if (error.request) {
        console.error('No response received:', error.request);
      }
      throw error
    }
  }
} 
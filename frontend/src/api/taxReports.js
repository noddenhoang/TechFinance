import { api } from './api';

/**
 * Tax reports API service
 */
export const taxReports = {
  /**
   * Get tax report data for a specific year
   * 
   * @param {number} year - The year to get data for
   * @returns {Promise<Object>} - Tax report data including income tax, expense tax, and difference data
   */
  async getTaxReport(year) {
    try {
      const response = await api.get(`/api/reports/tax/yearly?year=${year}`);
      
      // Transform the array of monthly reports into the format expected by the frontend
      const incomeTaxData = [
        { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0 },
        { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0 },
      ];
      
      const expenseTaxData = [
        { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0 },
        { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0 },
      ];
      
      const differenceData = [
        { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0 },
        { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0 },
      ];
      
      // Backend returns 12 monthly reports, we need to aggregate them by quarter
      if (response.data && Array.isArray(response.data)) {
        response.data.forEach(report => {
          const month = report.month;
          const quarter = Math.ceil(month / 3);
          
          if (quarter >= 1 && quarter <= 4) {
            // In this mock implementation, we're assuming all income tax is GTGT
            // and we're splitting expense tax between GTGT and "Khác" (Other)
            // This should be adapted based on the actual backend implementation
            
            // GTGT for income
            incomeTaxData[0][`q${quarter}`] += report.incomeTax;
            
            // Split expense tax between GTGT (70%) and Khác (30%)
            expenseTaxData[0][`q${quarter}`] += report.expenseTax * 0.7;
            expenseTaxData[1][`q${quarter}`] += report.expenseTax * 0.3;
            
            // Calculate difference
            differenceData[0][`q${quarter}`] += incomeTaxData[0][`q${quarter}`] - expenseTaxData[0][`q${quarter}`];
            differenceData[1][`q${quarter}`] += - expenseTaxData[1][`q${quarter}`];
          }
        });
      }
      
      return {
        incomeTax: incomeTaxData,
        expenseTax: expenseTaxData,
        difference: differenceData
      };
    } catch (error) {
      console.error('Error fetching tax report:', error);
      throw error;
    }
  },

  /**
   * Get tax report data for a specific month and year
   * 
   * @param {number} year - The year to get data for
   * @param {number} month - The month to get data for (1-12)
   * @returns {Promise<Object>} - Tax report data for the specified month
   */
  async getMonthlyTaxReport(year, month) {
    try {
      const response = await api.get(`/api/reports/tax/monthly?year=${year}&month=${month}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching monthly tax report:', error);
      throw error;
    }
  },
  
  /**
   * Get tax report data for a specific quarter and year
   * 
   * @param {number} year - The year to get data for
   * @param {number} quarter - The quarter to get data for (1-4)
   * @returns {Promise<Object>} - Tax report data for the specified quarter
   */
  async getQuarterlyTaxReport(year, quarter) {
    try {
      // This endpoint doesn't exist directly, so we need to fetch the yearly data
      // and filter for the specific quarter
      const yearlyData = await this.getTaxReport(year);
      
      // Create a subset of the data for the specific quarter
      const quarterlyData = {
        incomeTax: yearlyData.incomeTax.map(item => ({
          type: item.type,
          amount: item[`q${quarter}`]
        })),
        expenseTax: yearlyData.expenseTax.map(item => ({
          type: item.type,
          amount: item[`q${quarter}`]
        })),
        difference: yearlyData.difference.map(item => ({
          type: item.type,
          amount: item[`q${quarter}`]
        }))
      };
      
      return quarterlyData;
    } catch (error) {
      console.error('Error fetching quarterly tax report:', error);
      throw error;
    }
  }
};

export default taxReports; 
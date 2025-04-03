/**
 * Format a number as currency
 * @param {number|string} value - The number to format
 * @param {string} locale - Locale for formatting (default: 'vi-VN')
 * @param {string} currency - Currency code (default: 'VND')
 * @returns {string} Formatted currency string
 */
export function formatCurrency(value, locale = 'vi-VN', currency = 'VND') {
  if (value === null || value === undefined) {
    return '';
  }
  
  // Convert string to number if needed
  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  // Return empty string for invalid numbers
  if (isNaN(numValue)) {
    return '';
  }
  
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(numValue);
}

/**
 * Format a date string to localized format
 * @param {string} dateString - ISO date string
 * @param {string} locale - Locale for formatting (default: 'vi-VN')
 * @returns {string} Formatted date string
 */
export function formatDate(dateString, locale = 'vi-VN') {
  if (!dateString) {
    return '';
  }
  
  const date = new Date(dateString);
  
  // Check if date is valid
  if (isNaN(date.getTime())) {
    return '';
  }
  
  return date.toLocaleDateString(locale, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

/**
 * Định dạng ngày giờ theo định dạng Việt Nam (DD/MM/YYYY HH:MM)
 * @param {string|Date} datetime - Ngày giờ cần định dạng
 * @returns {string} Chuỗi ngày giờ đã định dạng
 */
export function formatDateTime(datetime) {
  if (!datetime) return '';
  
  const dateObj = typeof datetime === 'string' ? new Date(datetime) : datetime;
  
  return dateObj.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Chuyển đổi số thành chuỗi định dạng phân cách hàng nghìn
 * @param {number} value - Số cần định dạng
 * @returns {string} Chuỗi đã được định dạng
 */
export function formatNumber(value) {
  if (value === null || value === undefined) return '';
  return new Intl.NumberFormat('vi-VN').format(value);
}
/**
 * Định dạng số tiền theo định dạng tiền tệ Việt Nam
 * @param {number} amount - Số tiền cần định dạng
 * @param {string} currency - Đơn vị tiền tệ (mặc định là VND)
 * @returns {string} Chuỗi định dạng
 */
export function formatCurrency(amount, currency = 'VND') {
  if (amount === null || amount === undefined) return '';
  
  const formatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 0
  });
  
  return formatter.format(amount);
}

/**
 * Định dạng ngày theo định dạng Việt Nam (DD/MM/YYYY)
 * @param {string|Date} date - Ngày cần định dạng
 * @returns {string} Chuỗi ngày đã định dạng
 */
export function formatDate(date) {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  return dateObj.toLocaleDateString('vi-VN', {
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
<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue'
import Chart from 'chart.js/auto'
import { receivablePayable } from '../../api/receivablePayable'

// Current year for filter selection
const currentYear = new Date().getFullYear();

// Generate years from 2020 to current year
const years = computed(() => {
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

// Filters
const filters = reactive({
  year: currentYear
});

// Change year
const changeYear = () => {
  // Will be implemented to load data when the report is fully developed
  console.log('Year changed to:', filters.year);
  fetchData();
};

// Will load data based on selected year
const fetchData = async () => {
  try {
    console.log('Starting data fetch...');
    error.value = null;
    loading.value = true;
    
    const data = await receivablePayable.getChartData({ year: filters.year });
    console.log('Data received:', data);
    
    // Update reactive data
    reportData.totalReceived = data.totalReceived || 0;
    reportData.totalPending = data.totalPending || 0;
    reportData.totalReceivable = data.totalReceivable || 0;
    reportData.totalPaid = data.totalPaid || 0;
    reportData.totalUnpaid = data.totalUnpaid || 0;
    reportData.totalPayable = data.totalPayable || 0;
    reportData.receivablesByMonth = data.receivablesByMonth || [];
    reportData.payablesByMonth = data.payablesByMonth || [];
    
    console.log('Updated reportData:', reportData);
    
    // Important: Give the DOM time to render before accessing canvas elements
    loading.value = false;
    
    // Wait for the next tick when DOM has updated before rendering charts
    nextTick(() => {
      console.log('On nextTick - attempting to render charts');
      // Double check that we're out of loading state
      if (!loading.value) {
        renderCharts();
      }
    });
  } catch (err) {
    console.error('Error fetching report data:', err);
    
    let errorMessage = 'Không thể tải dữ liệu báo cáo';
    
    if (err.response) {
      // Server returned an error response
      if (err.response.status === 401) {
        errorMessage = 'Bạn không có quyền truy cập dữ liệu này';
      } else if (err.response.status === 404) {
        errorMessage = 'Không tìm thấy API endpoint';
      } else if (err.response.status >= 500) {
        errorMessage = 'Lỗi máy chủ: ' + (err.response.data?.message || 'Lỗi không xác định');
      } else {
        errorMessage = 'Lỗi: ' + (err.response.data?.message || err.message || 'Không xác định');
      }
    } else if (err.request) {
      // No response received from server
      errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra xem backend đã được khởi chạy chưa.';
    } else {
      // Something happened in setting up the request
      errorMessage = 'Lỗi khi gửi yêu cầu: ' + (err.message || 'Không xác định');
    }
    
    error.value = errorMessage;
    loading.value = false;
  }
};

// Vars
const loading = ref(true)
const error = ref(null)
const reportData = reactive({
  totalReceived: 0,
  totalPending: 0,
  totalReceivable: 0,
  totalPaid: 0,
  totalUnpaid: 0,
  totalPayable: 0,
  receivablesByMonth: [],
  payablesByMonth: []
})

// Chart refs
const receivableChart = ref(null)
const payableChart = ref(null)
const receivableMonthlyChart = ref(null)
const payableMonthlyChart = ref(null)

// Chart instances
let receivableChartInstance = null
let payableChartInstance = null
let receivableMonthlyChartInstance = null
let payableMonthlyChartInstance = null

// Chart rendering
const renderCharts = () => {
  renderReceivableDonutChart()
  renderPayableDonutChart()
  renderReceivableMonthlyChart()
  renderPayableMonthlyChart()
}

const renderReceivableDonutChart = () => {
  try {
    if (!receivableChart.value) {
      console.error('Receivable chart DOM element not available');
      return;
    }
    
    if (receivableChartInstance) {
      receivableChartInstance.destroy();
    }
    
    const ctx = receivableChart.value.getContext('2d');
    if (!ctx) {
      console.error('Failed to get 2D context for receivable chart');
      return;
    }
    
    console.log('Creating receivable donut chart with data:', [reportData.totalReceived, reportData.totalPending]);
    
    receivableChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Đã nhận', 'Còn phải thu'],
        datasets: [{
          data: [reportData.totalReceived, reportData.totalPending],
          backgroundColor: ['#8884f8', '#e5e9f2'],
          borderWidth: 0,
          cutout: '75%'
        }]
      },
      options: {
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                return `${context.label}: ${formatCurrency(context.raw)}`;
              }
            }
          }
        },
        responsive: true,
        maintainAspectRatio: false
      }
    });
    
    console.log('Receivable donut chart created successfully');
  } catch (err) {
    console.error('Error rendering receivable donut chart:', err);
  }
}

const renderPayableDonutChart = () => {
  try {
    if (!payableChart.value) {
      console.error('Payable chart DOM element not available');
      return;
    }
    
    if (payableChartInstance) {
      payableChartInstance.destroy();
    }
    
    const ctx = payableChart.value.getContext('2d');
    if (!ctx) {
      console.error('Failed to get 2D context for payable chart');
      return;
    }
    
    console.log('Creating payable donut chart with data:', [reportData.totalPaid, reportData.totalUnpaid]);
    
    payableChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Đã thanh toán', 'Còn phải trả'],
        datasets: [{
          data: [reportData.totalPaid, reportData.totalUnpaid],
          backgroundColor: ['#f88686', '#e5e9f2'],
          borderWidth: 0,
          cutout: '75%'
        }]
      },
      options: {
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                return `${context.label}: ${formatCurrency(context.raw)}`;
              }
            }
          }
        },
        responsive: true,
        maintainAspectRatio: false
      }
    });
    
    console.log('Payable donut chart created successfully');
  } catch (err) {
    console.error('Error rendering payable donut chart:', err);
  }
}

const renderReceivableMonthlyChart = () => {
  try {
    if (!receivableMonthlyChart.value) {
      console.error('Receivable monthly chart DOM element not available');
      return;
    }
    
    if (receivableMonthlyChartInstance) {
      receivableMonthlyChartInstance.destroy();
    }
    
    const ctx = receivableMonthlyChart.value.getContext('2d');
    if (!ctx) {
      console.error('Failed to get 2D context for receivable monthly chart');
      return;
    }
    
    const labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
    const monthlyData = new Array(12).fill(0);
    
    // Map the data from API to array format
    if (reportData.receivablesByMonth && reportData.receivablesByMonth.length > 0) {
      reportData.receivablesByMonth.forEach(item => {
        if (item.month >= 1 && item.month <= 12) {
          monthlyData[item.month - 1] = item.amount;
        }
      });
    }
    
    console.log('Creating receivable monthly chart with data:', monthlyData);
    
    receivableMonthlyChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Phải thu',
          data: monthlyData,
          backgroundColor: '#8884f8'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                if (value >= 1000000) {
                  return (value / 1000000).toFixed(0) + 'đ';
                }
                return value + 'đ';
              }
            }
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    });
    
    console.log('Receivable monthly chart created successfully');
  } catch (err) {
    console.error('Error rendering receivable monthly chart:', err);
  }
}

const renderPayableMonthlyChart = () => {
  try {
    if (!payableMonthlyChart.value) {
      console.error('Payable monthly chart DOM element not available');
      return;
    }
    
    if (payableMonthlyChartInstance) {
      payableMonthlyChartInstance.destroy();
    }
    
    const ctx = payableMonthlyChart.value.getContext('2d');
    if (!ctx) {
      console.error('Failed to get 2D context for payable monthly chart');
      return;
    }
    
    const labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
    const monthlyData = new Array(12).fill(0);
    
    // Map the data from API to array format
    if (reportData.payablesByMonth && reportData.payablesByMonth.length > 0) {
      reportData.payablesByMonth.forEach(item => {
        if (item.month >= 1 && item.month <= 12) {
          monthlyData[item.month - 1] = item.amount;
        }
      });
    }
    
    console.log('Creating payable monthly chart with data:', monthlyData);
    
    payableMonthlyChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Phải trả',
          data: monthlyData,
          backgroundColor: '#f88686'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                if (value >= 1000000) {
                  return (value / 1000000).toFixed(0) + 'đ';
                }
                return value + 'đ';
              }
            }
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    });
    
    console.log('Payable monthly chart created successfully');
  } catch (err) {
    console.error('Error rendering payable monthly chart:', err);
  }
}

// Utility functions
const formatCurrency = (value) => {
  const amount = Number(value) || 0
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
    .format(amount)
    .replace(/\s/g, '')
}

const calculateReceivedPercentage = () => {
  if (!reportData.totalReceivable || reportData.totalReceivable === 0) return 100
  const percentage = (reportData.totalReceived / reportData.totalReceivable) * 100
  return Math.round(percentage)
}

const calculatePaidPercentage = () => {
  if (!reportData.totalPayable || reportData.totalPayable === 0) return 100
  const percentage = (reportData.totalPaid / reportData.totalPayable) * 100
  return Math.round(percentage)
}

// Initialize on component mount
onMounted(() => {
  console.log('Component mounted - starting data fetch');
  try {
    // Set initial data
    reportData.totalReceived = 0;
    reportData.totalPending = 0;
    reportData.totalReceivable = 0;
    reportData.totalPaid = 0; 
    reportData.totalUnpaid = 0;
    reportData.totalPayable = 0;
    reportData.receivablesByMonth = [];
    reportData.payablesByMonth = [];
    
    // Fetch data from API
    fetchData();
  } catch (err) {
    console.error('Error during component initialization:', err);
    error.value = 'Lỗi khởi tạo: ' + (err.message || 'Unknown error');
    loading.value = false;
  }
})
</script>

<template>
  <AppLayout>
    <template #title>
      <h1 class="mb-3">Báo cáo Phải Thu/Phải Trả</h1>
    </template>

    <div class="content-container">
      <h2>Báo cáo phải thu & phải trả</h2>
      <p>Trang này hiển thị tổng hợp số liệu về các khoản đã thanh toán và chưa thanh toán (phải thu, phải trả).</p>
      
      <!-- Filter container with consistent design -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel-fill"></i> Bộ lọc báo cáo
          </h3>
        </div>
        <div class="filter-content">
          <div class="filter-grid">
            <div class="form-group">
              <label class="form-label">Năm</label>
              <select v-model="filters.year" class="form-select" @change="changeYear">
                <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-spinner-container">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu báo cáo...</p>
      </div>

      <div v-else-if="error" class="error-message">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
        <button @click="fetchData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>

      <div v-else-if="reportData.totalReceivable !== undefined" class="report-container">
        <h2 class="report-section-title">8.1. Phải thu & phải trả</h2>
        <p class="report-section-subtitle">Thông tin tổng hợp các khoản phải thu và phải trả năm {{ filters.year }}</p>
        
        <div class="row">
          <!-- Phải thu section -->
          <div class="col-md-6">
            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">Báo cáo phải thu</h3>
              </div>
              <div class="card-body">
                <div class="table-responsive">
                  <table class="table table-bordered">
                    <thead class="table-light">
                      <tr>
                        <th scope="col">Khoản thanh toán đã nhận</th>
                        <th scope="col">Khoản phải thu</th>
                        <th scope="col">Tổng</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>{{ formatCurrency(reportData.totalReceived) }}</td>
                        <td>{{ formatCurrency(reportData.totalPending) }}</td>
                        <td>{{ formatCurrency(reportData.totalReceivable) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div class="row">
                  <div class="col-md-12 text-center">
                    <h6>% Thanh toán đã nhận</h6>
                    <div class="chart-container" style="position: relative; height: 200px; width: 100%">
                      <canvas ref="receivableChart"></canvas>
                      <div class="chart-percent">{{ calculateReceivedPercentage() }}%</div>
                    </div>
                  </div>
                </div>

                <div class="mt-4">
                  <h6>Các khoản phải thu mỗi tháng</h6>
                  <div style="height: 200px">
                    <canvas ref="receivableMonthlyChart"></canvas>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Phải trả section -->
          <div class="col-md-6">
            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">Báo cáo phải trả</h3>
              </div>
              <div class="card-body">
                <div class="table-responsive">
                  <table class="table table-bordered">
                    <thead class="table-light">
                      <tr>
                        <th scope="col">Đã thanh toán</th>
                        <th scope="col">Nợ phải trả</th>
                        <th scope="col">Tổng</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>{{ formatCurrency(reportData.totalPaid) }}</td>
                        <td>{{ formatCurrency(reportData.totalUnpaid) }}</td>
                        <td>{{ formatCurrency(reportData.totalPayable) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div class="row">
                  <div class="col-md-12 text-center">
                    <h6>% Chi phí đã thanh toán</h6>
                    <div class="chart-container" style="position: relative; height: 200px; width: 100%">
                      <canvas ref="payableChart"></canvas>
                      <div class="chart-percent">{{ calculatePaidPercentage() }}%</div>
                    </div>
                  </div>
                </div>

                <div class="mt-4">
                  <h6>Các khoản phải trả mỗi tháng</h6>
                  <div style="height: 200px">
                    <canvas ref="payableMonthlyChart"></canvas>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="no-data">
        <i class="bi bi-info-circle"></i>
        <p>Không có dữ liệu phải thu/trả để hiển thị.</p>
        <button @click="fetchData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-container {
  background-color: white;
  padding: 1rem 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.content-container h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 1rem;
}

.content-container p {
  color: #6b7280;
  line-height: 1.5;
}

/* Filter styles */
.filter-container {
  margin-top: 1.5rem;
  margin-bottom: 2rem;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.filter-header {
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin: 0;
}

.card-title i {
  margin-right: 0.5rem;
}

.filter-content {
  padding: 1.5rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
}

.form-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}

.chart-container {
  position: relative;
}

.chart-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 1.5rem;
  font-weight: bold;
}

table th {
  text-align: center;
}

h6 {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.report-container {
  margin-top: 2rem;
}

.report-section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.report-section-subtitle {
  color: #6b7280;
  margin-bottom: 2rem;
}

.card-header {
  background-color: #f9fafb;
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.card-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.loading-spinner-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message, .no-data {
  text-align: center;
  padding: 2rem;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  margin: 2rem 0;
}

.error-message i, .no-data i {
  font-size: 2rem;
  margin-bottom: 1rem;
  display: block;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-weight: 500;
  cursor: pointer;
  margin-top: 1rem;
}

.btn-primary:hover {
  background-color: #2563eb;
}
</style> 
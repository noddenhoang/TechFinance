<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { financialReports } from '../../api/financialReports';
import Chart from 'chart.js/auto';

// State
const isLoading = ref(true);
const error = ref(null);
const data = ref(null);
const cashFlowChart = ref(null);
const currentYear = new Date().getFullYear();

// Filters
const filters = reactive({
  year: currentYear
});

// Computed years for dropdown (2020 to current year)
const years = computed(() => {
  const yearList = [];
  for (let year = 2020; year <= currentYear + 1; year++) {
    yearList.push(year);
  }
  return yearList;
});

// Helper function for formatting currency
const formatCurrency = (value) => {
  if (!value && value !== 0) return '- đ';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

// Change year and reload data
const changeYear = (event) => {
  filters.year = parseInt(event.target.value);
  loadData();
};

// Load data
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Get data from the API
    const apiData = await financialReports.getCashFlow(filters.year);
    
    // Transform BigDecimal values from string (Java) to numbers (JavaScript)
    data.value = {
      year: apiData.year,
      summary: {
        totalIncome: Number(apiData.summary.totalIncome),
        totalExpense: Number(apiData.summary.totalExpense),
        totalProfit: Number(apiData.summary.totalProfit),
        endBalance: Number(apiData.summary.endBalance)
      },
      monthlyData: apiData.monthlyData.map(item => ({
        month: item.month,
        income: Number(item.income),
        expense: Number(item.expense),
        profit: Number(item.profit),
        balance: Number(item.balance)
      }))
    };
    
    nextTick(() => {
      setTimeout(() => {
        renderCashFlowChart();
      }, 100);
    });
  } catch (err) {
    console.error('Error loading cash flow data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo dòng tiền. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Render cash flow chart
const renderCashFlowChart = () => {
  if (!data.value) return;
  
  nextTick(() => {
    try {
      // Destroy existing chart if it exists
      if (cashFlowChart.value) {
        cashFlowChart.value.destroy();
      }
      
      const ctx = document.getElementById('cashFlowChart');
      if (ctx) {
        const labels = data.value.monthlyData.map(item => `T${item.month}`);
        const incomeData = data.value.monthlyData.map(item => item.income);
        const expenseData = data.value.monthlyData.map(item => item.expense);
        const balanceData = data.value.monthlyData.map(item => item.balance);
        
        cashFlowChart.value = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [
              {
                label: 'Thu nhập',
                data: incomeData,
                backgroundColor: 'rgba(79, 70, 229, 0.8)',
                order: 2
              },
              {
                label: 'Chi phí',
                data: expenseData,
                backgroundColor: 'rgba(239, 68, 68, 0.8)',
                order: 2
              },
              {
                label: 'Số tiền còn lại',
                data: balanceData,
                borderColor: 'rgba(0, 0, 0, 1)',
                borderWidth: 2,
                type: 'line',
                order: 1
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'bottom'
              },
              tooltip: {
                callbacks: {
                  label: function(context) {
                    let label = context.dataset.label || '';
                    if (label) {
                      label += ': ';
                      label += formatCurrency(context.raw);
                    }
                    return label;
                  }
                }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                ticks: {
                  callback: (value) => {
                    if (value >= 1000000) {
                      return (value / 1000000).toFixed(0) + ' triệu đ';
                    }
                    return formatCurrency(value);
                  }
                }
              }
            }
          }
        });
      } else {
        console.error('Cash flow chart element not found');
      }
    } catch (err) {
      console.error('Error rendering cash flow chart:', err);
    }
  });
};

// Lifecycle hooks
onMounted(() => {
  loadData();
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo - Dòng tiền</template>
    
    <div class="content-container">
      <div class="header">
        <h2>8. Báo cáo - Dòng tiền</h2>
      </div>
      
      <!-- Report Filters -->
      <div class="report-filters">
        <h3>Bộ lọc báo cáo</h3>
        <div class="filter-row">
          <div class="filter-group">
            <label for="yearFilter">Năm:</label>
            <select 
              id="yearFilter" 
              v-model="filters.year" 
              @change="changeYear"
              class="select-input"
            >
              <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
            </select>
          </div>
        </div>
      </div>
      
      <!-- Loading indicator -->
      <div v-if="isLoading" class="loading-spinner-container">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu báo cáo dòng tiền...</p>
      </div>
      
      <!-- Error message -->
      <div v-else-if="error" class="error-message">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
        <button @click="loadData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <!-- Cash Flow Report Content -->
      <div v-else-if="data" class="cash-flow-content">
        <!-- Summary -->
        <div class="summary-container">
          <div class="summary-item">
            <span class="summary-label">- đ</span>
            <span class="summary-value">{{ formatCurrency(data.summary.totalIncome) }}</span>
            <span class="summary-caption">Tổng Thu nhập</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">- đ</span>
            <span class="summary-value">{{ formatCurrency(data.summary.totalExpense) }}</span>
            <span class="summary-caption">Tổng Chi phí</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">- đ</span>
            <span class="summary-value">{{ formatCurrency(data.summary.totalProfit) }}</span>
            <span class="summary-caption">Lợi nhuận / Lỗ</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">- đ</span>
            <span class="summary-value">{{ formatCurrency(data.summary.endBalance) }}</span>
            <span class="summary-caption">Số tiền cuối kỳ</span>
          </div>
        </div>
        
        <!-- Chart -->
        <div class="chart-section">
          <div class="chart-container">
            <canvas id="cashFlowChart" height="300"></canvas>
          </div>
        </div>
        
        <!-- Monthly Data Table -->
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Tháng</th>
                <th>Thu nhập</th>
                <th>Chi phí</th>
                <th>Lợi nhuận / Lỗ</th>
                <th>Số tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in data.monthlyData" :key="item.month">
                <td>T{{ item.month }}</td>
                <td>{{ formatCurrency(item.income) }}</td>
                <td>{{ formatCurrency(item.expense) }}</td>
                <td>{{ formatCurrency(item.profit) }}</td>
                <td>{{ formatCurrency(item.balance) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>Total</td>
                <td>{{ formatCurrency(data.summary.totalIncome) }}</td>
                <td>{{ formatCurrency(data.summary.totalExpense) }}</td>
                <td>{{ formatCurrency(data.summary.totalProfit) }}</td>
                <td>{{ formatCurrency(data.summary.endBalance) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
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
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

h2 {
  color: #166534;
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  border-left: 4px solid #16a34a;
  padding-left: 0.75rem;
}

.report-filters {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.report-filters h3 {
  font-size: 1rem;
  font-weight: 600;
  margin-top: 0;
  margin-bottom: 1rem;
  color: #374151;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.select-input {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background-color: white;
  min-width: 120px;
}

.summary-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.summary-item {
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #e5e7eb;
}

.summary-label {
  font-size: 0.75rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.25rem;
}

.summary-caption {
  font-size: 0.875rem;
  color: #4b5563;
}

.chart-section {
  margin-bottom: 1.5rem;
}

.chart-container {
  height: 300px;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #fff;
  position: relative;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.data-table th,
.data-table td {
  border: 1px solid #e5e7eb;
  padding: 0.75rem;
  text-align: right;
}

.data-table th:first-child,
.data-table td:first-child {
  text-align: left;
}

.data-table th {
  background-color: #f3f4f6;
  font-weight: 600;
  color: #374151;
}

.data-table tfoot td {
  font-weight: 600;
  background-color: #f9fafb;
}

/* Loading and error styles */
.loading-spinner-container,
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 0;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin-bottom: 1rem;
  border: 4px solid #e5e7eb;
  border-radius: 50%;
  border-top-color: #3b82f6;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  color: #dc2626;
}

.error-message i {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  border: 1px solid #2563eb;
  background-color: #2563eb;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
}

@media (max-width: 768px) {
  .summary-container {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-container {
    margin-top: 1rem;
  }
}
</style>
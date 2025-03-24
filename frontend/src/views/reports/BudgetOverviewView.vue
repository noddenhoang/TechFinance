<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { budgetOverview } from '../../api/budgetOverview';
import Chart from 'chart.js/auto';

// State
const isLoading = ref(true);
const error = ref(null);
const data = ref(null);
const incomeChart = ref(null);
const expenseChart = ref(null);
const currentYear = new Date().getFullYear();

// Filters
const filters = reactive({
  year: currentYear,
  month: null, // If null, will load yearly data
});

// Helper function for formatting currency
const formatCurrency = (value) => {
  if (!value && value !== 0) return '0 đ';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

// Helper function for formatting percentage
const formatPercentage = (value) => {
  if (!value && value !== 0) return '0%';
  return `${Math.round(value)}%`;
};

// Get month name from month number
const getMonthName = (month) => {
  const monthNames = [
    'T1', 'T2', 'T3', 'T4', 'T5', 'T6',
    'T7', 'T8', 'T9', 'T10', 'T11', 'T12'
  ];
  return monthNames[month - 1];
};

// Change year and reload data
const changeYear = (year) => {
  filters.year = year;
  loadData();
};

// Load data
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    data.value = await budgetOverview.getBudgetOverview(filters.year, filters.month);
    nextTick(() => {
      updateCharts();
    });
  } catch (err) {
    console.error('Error loading budget overview:', err);
    if (err.response && err.response.data && err.response.data.error) {
      error.value = `Lỗi: ${err.response.data.error}`;
    } else if (err.message) {
      error.value = `Lỗi: ${err.message}`;
    } else {
      error.value = 'Không thể tải dữ liệu tổng quan ngân sách. Vui lòng thử lại sau.';
    }
  } finally {
    isLoading.value = false;
  }
};

// Update charts
const updateCharts = () => {
  if (!data.value) return;
  
  // Destroy existing charts
  if (incomeChart.value) {
    incomeChart.value.destroy();
  }
  
  if (expenseChart.value) {
    expenseChart.value.destroy();
  }
  
  // Setup income chart
  const incomeCtx = document.getElementById('incomeChart');
  if (incomeCtx) {
    incomeChart.value = new Chart(incomeCtx, {
      type: 'bar',
      data: {
        labels: data.value.income.monthlyData.map(m => getMonthName(m.month)),
        datasets: [{
          label: 'Thu nhập thực tế',
          data: data.value.income.monthlyData.map(m => m.actual),
          backgroundColor: 'rgba(79, 70, 229, 0.7)',
          borderColor: 'rgba(79, 70, 229, 1)',
          borderWidth: 1
        }]
      },
      options: {
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value) => {
                if (value >= 1000000) {
                  return (value / 1000000).toFixed(0) + ' tr';
                }
                return value;
              }
            }
          }
        },
        responsive: true,
        maintainAspectRatio: false
      }
    });
  }
  
  // Setup expense chart
  const expenseCtx = document.getElementById('expenseChart');
  if (expenseCtx) {
    expenseChart.value = new Chart(expenseCtx, {
      type: 'bar',
      data: {
        labels: data.value.expense.monthlyData.map(m => getMonthName(m.month)),
        datasets: [{
          label: 'Chi phí thực tế',
          data: data.value.expense.monthlyData.map(m => m.actual),
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: 'rgba(239, 68, 68, 1)',
          borderWidth: 1
        }]
      },
      options: {
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value) => {
                if (value >= 1000000) {
                  return (value / 1000000).toFixed(0) + ' tr';
                }
                return value;
              }
            }
          }
        },
        responsive: true,
        maintainAspectRatio: false
      }
    });
  }
};

// Get the data for the current month tables
const incomeTableData = computed(() => {
  if (!data.value || !data.value.income) return [];
  
  return data.value.income.monthlyData;
});

const expenseTableData = computed(() => {
  if (!data.value || !data.value.expense) return [];
  
  return data.value.expense.monthlyData;
});

// Lifecycle hooks
onMounted(() => {
  loadData();
});
</script>

<template>
  <AppLayout>
    <template #page-title>Tổng quan ngân sách</template>
    
    <div class="content-container">
      <h2>8.2. Tổng quan ngân sách</h2>
      
      <!-- Year selection -->
      <div class="filter-container">
        <div class="year-selector">
          <button 
            class="year-button" 
            :class="{ 'active': filters.year === currentYear - 1 }"
            @click="changeYear(currentYear - 1)"
          >
            {{ currentYear - 1 }}
          </button>
          <button 
            class="year-button" 
            :class="{ 'active': filters.year === currentYear }"
            @click="changeYear(currentYear)"
          >
            {{ currentYear }}
          </button>
          <button 
            class="year-button" 
            :class="{ 'active': filters.year === currentYear + 1 }"
            @click="changeYear(currentYear + 1)"
          >
            {{ currentYear + 1 }}
          </button>
        </div>
      </div>
      
      <!-- Loading indicator -->
      <div v-if="isLoading" class="loading-spinner-container">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu tổng quan ngân sách...</p>
      </div>
      
      <!-- Error message -->
      <div v-else-if="error" class="error-message">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
        <button @click="loadData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <!-- Budget Overview Content -->
      <div v-else-if="data" class="budget-overview-content">
        <!-- Budget Summary -->
        <div class="budget-summary-container">
          <!-- Income Summary -->
          <div class="budget-panel">
            <h3>Thu nhập</h3>
            
            <div class="budget-panel-content">
              <!-- Circular Progress -->
              <div class="circular-progress-container">
                <div class="circular-progress">
                  <div class="progress-ring" :style="{
                    background: `conic-gradient(#4F46E5 ${data.income.percentage}%, #E9ECEF ${data.income.percentage}% 100%)`
                  }">
                    <div class="progress-center">
                      <span class="progress-percentage">{{ formatPercentage(data.income.percentage) }}</span>
                      <span class="progress-label">Thu/Ngân sách</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Budget Data Table -->
              <div class="budget-data-table">
                <div class="budget-data-row">
                  <span class="budget-data-label">Thu nhập ngân sách</span>
                  <span class="budget-data-value">{{ formatCurrency(data.income.totalBudget) }}</span>
                </div>
                <div class="budget-data-row">
                  <span class="budget-data-label">Thu nhập thực tế</span>
                  <span class="budget-data-value">{{ formatCurrency(data.income.totalActual) }}</span>
                </div>
                <div class="budget-data-row">
                  <span class="budget-data-label">Chênh lệch</span>
                  <span class="budget-data-value">{{ formatCurrency(data.income.difference) }}</span>
                </div>
              </div>
            </div>
            
            <!-- Income Chart -->
            <div class="chart-container">
              <canvas id="incomeChart" height="200"></canvas>
            </div>
            
            <!-- Income Monthly Data Table -->
            <div class="monthly-table-container">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Tháng</th>
                    <th>Ngân sách</th>
                    <th>Thực tế</th>
                    <th>Chênh lệch</th>
                    <th>Thu nhập - % so với ngân sách</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in incomeTableData" :key="item.month">
                    <td>T{{ item.month }}</td>
                    <td>{{ formatCurrency(item.budget) }}</td>
                    <td>{{ formatCurrency(item.actual) }}</td>
                    <td>{{ formatCurrency(item.difference) }}</td>
                    <td>
                      <div class="progress-bar-container">
                        <div class="progress-bar" :style="{ width: `${Math.min(item.percentage, 100)}%`, backgroundColor: '#4F46E5' }"></div>
                        <span>{{ formatPercentage(item.percentage) }}</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td>Tổng</td>
                    <td>{{ formatCurrency(data.income.totalBudget) }}</td>
                    <td>{{ formatCurrency(data.income.totalActual) }}</td>
                    <td>{{ formatCurrency(data.income.difference) }}</td>
                    <td>{{ formatPercentage(data.income.percentage) }}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
          
          <!-- Expense Summary -->
          <div class="budget-panel">
            <h3>Chi phí</h3>
            
            <div class="budget-panel-content">
              <!-- Circular Progress -->
              <div class="circular-progress-container">
                <div class="circular-progress">
                  <div class="progress-ring" :style="{
                    background: `conic-gradient(#EF4444 ${data.expense.percentage}%, #E9ECEF ${data.expense.percentage}% 100%)`
                  }">
                    <div class="progress-center">
                      <span class="progress-percentage">{{ formatPercentage(data.expense.percentage) }}</span>
                      <span class="progress-label">Chi phí / Dự chi</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Budget Data Table -->
              <div class="budget-data-table">
                <div class="budget-data-row">
                  <span class="budget-data-label">Chi phí ngân sách</span>
                  <span class="budget-data-value">{{ formatCurrency(data.expense.totalBudget) }}</span>
                </div>
                <div class="budget-data-row">
                  <span class="budget-data-label">Chi phí thực tế</span>
                  <span class="budget-data-value">{{ formatCurrency(data.expense.totalActual) }}</span>
                </div>
                <div class="budget-data-row">
                  <span class="budget-data-label">Chênh lệch</span>
                  <span class="budget-data-value">{{ formatCurrency(data.expense.difference) }}</span>
                </div>
              </div>
            </div>
            
            <!-- Expense Chart -->
            <div class="chart-container">
              <canvas id="expenseChart" height="200"></canvas>
            </div>
            
            <!-- Expense Monthly Data Table -->
            <div class="monthly-table-container">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Tháng</th>
                    <th>Ngân sách</th>
                    <th>Thực tế</th>
                    <th>Chênh lệch</th>
                    <th>Chi phí - % so với ngân sách</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in expenseTableData" :key="item.month">
                    <td>T{{ item.month }}</td>
                    <td>{{ formatCurrency(item.budget) }}</td>
                    <td>{{ formatCurrency(item.actual) }}</td>
                    <td>{{ formatCurrency(item.difference) }}</td>
                    <td>
                      <div class="progress-bar-container">
                        <div class="progress-bar" :style="{ width: `${Math.min(item.percentage, 100)}%`, backgroundColor: '#EF4444' }"></div>
                        <span>{{ formatPercentage(item.percentage) }}</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td>Tổng</td>
                    <td>{{ formatCurrency(data.expense.totalBudget) }}</td>
                    <td>{{ formatCurrency(data.expense.totalActual) }}</td>
                    <td>{{ formatCurrency(data.expense.difference) }}</td>
                    <td>{{ formatPercentage(data.expense.percentage) }}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
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

h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 1.5rem;
}

.budget-summary-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.budget-panel {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
}

.budget-panel h3 {
  padding: 1rem;
  margin: 0;
  border-bottom: 1px solid #e5e7eb;
  font-size: 1.1rem;
  color: #374151;
  background-color: #f9fafb;
}

.budget-panel-content {
  display: flex;
  padding: 1.5rem;
}

.circular-progress-container {
  flex: 0 0 40%;
}

.circular-progress {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto;
}

.progress-ring {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.progress-center {
  position: absolute;
  width: 80%;
  height: 80%;
  border-radius: 50%;
  background-color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.progress-percentage {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
}

.progress-label {
  font-size: 0.7rem;
  color: #6b7280;
  text-align: center;
}

.budget-data-table {
  flex: 1;
  padding-left: 1.5rem;
}

.budget-data-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.budget-data-label {
  color: #6b7280;
  font-size: 0.9rem;
}

.budget-data-value {
  font-weight: 600;
  color: #111827;
}

.chart-container {
  height: 200px;
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
}

.monthly-table-container {
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
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
  padding: 0.5rem;
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

.progress-bar-container {
  position: relative;
  height: 20px;
  background-color: #f3f4f6;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  transition: width 0.3s ease;
}

.progress-bar-container span {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #374151;
  font-size: 0.8rem;
  font-weight: 600;
}

/* Loading, error, and no data states */
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

/* Responsive adjustments */
@media (max-width: 1024px) {
  .budget-summary-container {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .budget-panel-content {
    flex-direction: column;
  }
  
  .circular-progress-container {
    margin-bottom: 1.5rem;
  }
  
  .budget-data-table {
    padding-left: 0;
  }
}

/* Add new styles */
.filter-container {
  margin-bottom: 2rem;
  display: flex;
  justify-content: center;
}

.year-selector {
  display: flex;
  gap: 0.5rem;
}

.year-button {
  padding: 0.5rem 1rem;
  background-color: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.year-button.active {
  background-color: #2563eb;
  color: white;
  border-color: #2563eb;
}

.year-button:hover:not(.active) {
  background-color: #e5e7eb;
}
</style> 
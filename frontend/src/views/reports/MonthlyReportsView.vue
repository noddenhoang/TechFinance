<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { financialReports } from '../../api/financialReports';
import Chart from 'chart.js/auto';

// State for filters
const filters = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  categoryId: null
});

const chartType = ref('bar');

// Data state
const reportData = ref(null);
const isLoading = ref(false);
const error = ref(null);

// Chart references
const incomeChartRef = ref(null);
const expenseChartRef = ref(null);
const comparisonChartRef = ref(null);
let incomeChart = null;
let expenseChart = null;
let comparisonChart = null;

// State theo dõi các dataset đang ẩn
const hiddenDatasets = reactive({
  income: [1], // Ban đầu ẩn dataset Thực tế
  expense: [1],
  comparison: []
});

// Hàm chuyển đổi hiển thị dataset
const toggleDataset = (chartName, datasetIndex) => {
  if (!hiddenDatasets[chartName]) {
    hiddenDatasets[chartName] = [];
  }
  
  const index = hiddenDatasets[chartName].indexOf(datasetIndex);
  
  if (index === -1) {
    // Nếu chưa ẩn thì thêm vào danh sách ẩn
    hiddenDatasets[chartName].push(datasetIndex);
  } else {
    // Nếu đang ẩn thì bỏ khỏi danh sách ẩn
    hiddenDatasets[chartName].splice(index, 1);
  }
  
  // Cập nhật trạng thái hiển thị của dataset
  updateChartVisibility(chartName);
};

// Cập nhật hiển thị chart
const updateChartVisibility = (chartName) => {
  let chart;
  
  switch(chartName) {
    case 'income':
      chart = incomeChart;
      break;
    case 'expense':
      chart = expenseChart;
      break;
    case 'comparison':
      chart = comparisonChart;
      break;
  }
  
  if (!chart) return;
  
  chart.data.datasets.forEach((dataset, i) => {
    dataset.hidden = hiddenDatasets[chartName].includes(i);
  });
  
  chart.update();
};

// Computed lists
const months = computed(() => [
  { id: 1, name: 'Tháng 1' },
  { id: 2, name: 'Tháng 2' },
  { id: 3, name: 'Tháng 3' },
  { id: 4, name: 'Tháng 4' },
  { id: 5, name: 'Tháng 5' },
  { id: 6, name: 'Tháng 6' },
  { id: 7, name: 'Tháng 7' },
  { id: 8, name: 'Tháng 8' },
  { id: 9, name: 'Tháng 9' },
  { id: 10, name: 'Tháng 10' },
  { id: 11, name: 'Tháng 11' },
  { id: 12, name: 'Tháng 12' }
]);

const years = computed(() => {
  const currentYear = new Date().getFullYear();
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

const incomeCategories = computed(() => {
  if (!reportData.value?.incomeCategories) return [];
  return reportData.value.incomeCategories;
});

const expenseCategories = computed(() => {
  if (!reportData.value?.expenseCategories) return [];
  return reportData.value.expenseCategories;  
});

const filteredData = computed(() => {
  if (!reportData.value) return null;
  
  if (!filters.categoryId) {
    return reportData.value; // Trả về tất cả dữ liệu nếu không lọc
  }
  
  const result = {
    ...reportData.value,
    summary: { ...reportData.value.summary }
  };
  
  if (filters.categoryId.type === 'income') {
    // Lọc danh mục thu nhập
    const category = incomeCategories.value.find(c => c.categoryId === filters.categoryId.id);
    if (category) {
      result.incomeCategories = [category];
      result.summary.totalIncomeBudget = category.budgetAmount;
      result.summary.totalIncomeActual = category.actualAmount;
    }
  } else if (filters.categoryId.type === 'expense') {
    // Lọc danh mục chi tiêu
    const category = expenseCategories.value.find(c => c.categoryId === filters.categoryId.id);
    if (category) {
      result.expenseCategories = [category];
      result.summary.totalExpenseBudget = category.budgetAmount;
      result.summary.totalExpenseActual = category.actualAmount;
    }
  }
  
  return result;
});

// Methods
// Tải dữ liệu và render biểu đồ
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Gọi API để lấy dữ liệu báo cáo
    reportData.value = await financialReports.getMonthlyReport(filters.year, filters.month);
    
    // Đợi DOM cập nhật trước khi render biểu đồ
    await nextTick();
    // Sau đó mới render biểu đồ
    renderCharts();
  } catch (err) {
    console.error('Error loading report data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

const applyFilter = () => {
  loadData();
};

const renderCharts = () => {
  // Hủy biểu đồ cũ nếu đã tồn tại
  if (incomeChart) incomeChart.destroy();
  if (expenseChart) incomeChart.destroy();
  if (comparisonChart) incomeChart.destroy();
  
  // Nếu không có dữ liệu, không cần render
  if (!reportData.value) return;

  // ĐỢI LÂU HƠN để đảm bảo DOM đã cập nhật hoàn toàn
  setTimeout(() => {
    try {
      // Render từng biểu đồ riêng biệt và xử lý lỗi độc lập
      if (comparisonChartRef.value) {
        try {
          renderComparisonChart();
        } catch (error) {
          console.error('Error rendering comparison chart:', error);
        }
      }
      
      if (incomeChartRef.value) {
        try {
          renderIncomeChart();
        } catch (error) {
          console.error('Error rendering income chart:', error);
        }
      }
      
      if (expenseChartRef.value) {
        try {
          renderExpenseChart();
        } catch (error) {
          console.error('Error rendering expense chart:', error);
        }
      }
    } catch (error) {
      console.error('Error in chart rendering:', error);
    }
  }, 300); // Đợi đủ thời gian để DOM được render
};

// Cập nhật hàm renderIncomeChart
const renderIncomeChart = () => {
  // Kiểm tra lại lần nữa để đảm bảo an toàn
  if (!incomeChartRef.value) return;
  
  const ctx = incomeChartRef.value.getContext('2d');
  if (!ctx) return;
  
  // Dữ liệu đã được lọc nếu có chọn danh mục
  const categories = filteredData.value.incomeCategories;
  
  // Chỉ lấy top 5 danh mục nếu là biểu đồ cột và có quá nhiều danh mục
  const topCategories = chartType.value === 'bar' && categories.length > 5 
    ? categories.sort((a, b) => b.actualAmount - a.actualAmount).slice(0, 5)
    : categories;
  
  // Cấu hình chung
  const chartConfig = {
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: chartType.value === 'bar' ? 'top' : 'right',
        },
        title: {
          display: true,
          text: 'Thu nhập theo danh mục: Kế hoạch vs Thực tế'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              let label = context.dataset.label || '';
              if (label) {
                label += ': ';
              }
              label += formatCurrency(context.raw);
              return label;
            }
          }
        }
      }
    }
  };
  
  // Cấu hình riêng cho biểu đồ cột
  if (chartType.value === 'bar') {
    chartConfig.type = 'bar';
    chartConfig.data = {
      labels: topCategories.map(cat => cat.categoryName),
      datasets: [
        {
          label: 'Kế hoạch thu nhập',
          data: topCategories.map(cat => cat.budgetAmount),
          backgroundColor: 'rgba(79, 70, 229, 0.6)',
          borderColor: 'rgba(79, 70, 229, 1)',
          borderWidth: 1
        },
        {
          label: 'Thu nhập thực tế',
          data: topCategories.map(cat => cat.actualAmount),
          backgroundColor: 'rgba(16, 185, 129, 0.6)',
          borderColor: 'rgba(16, 185, 129, 1)',
          borderWidth: 1
        }
      ]
    };
    
    chartConfig.options.scales = {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function(value) {
            return formatCurrency(value);
          }
        }
      }
    };
  } 
  // Cấu hình cho biểu đồ tròn/vòng
  else {
    chartConfig.type = chartType.value; // 'pie' hoặc 'doughnut'
    chartConfig.data = {
      labels: topCategories.map(cat => cat.categoryName),
      datasets: [
        {
          label: 'Kế hoạch',
          data: topCategories.map(cat => cat.budgetAmount),
          backgroundColor: [
            'rgba(79, 70, 229, 0.7)',
            'rgba(16, 185, 129, 0.7)',
            'rgba(245, 158, 11, 0.7)',
            'rgba(239, 68, 68, 0.7)',
            'rgba(59, 130, 246, 0.7)',
            'rgba(168, 85, 247, 0.7)',
            'rgba(236, 72, 153, 0.7)',
          ],
          borderWidth: 1,
          hidden: false
        },
        {
          label: 'Thực tế',
          data: topCategories.map(cat => cat.actualAmount),
          backgroundColor: [
            'rgba(79, 70, 229, 0.9)',
            'rgba(16, 185, 129, 0.9)',
            'rgba(245, 158, 11, 0.9)',
            'rgba(239, 68, 68, 0.9)',
            'rgba(59, 130, 246, 0.9)',
            'rgba(168, 85, 247, 0.9)',
            'rgba(236, 72, 153, 0.9)',
          ],
          borderWidth: 1,
          hidden: true
        }
      ]
    };
  }
  
  incomeChart = new Chart(ctx, chartConfig);
  updateChartVisibility('income');
};

const renderExpenseChart = () => {
  // Kiểm tra lại lần nữa để đảm bảo an toàn
  if (!expenseChartRef.value) return;
  
  const ctx = expenseChartRef.value.getContext('2d');
  if (!ctx) return;
  
  // Dữ liệu đã được lọc nếu có chọn danh mục
  const categories = filteredData.value.expenseCategories;
  
  // Chỉ lấy top 5 danh mục nếu là biểu đồ cột và có quá nhiều danh mục
  const topCategories = chartType.value === 'bar' && categories.length > 5 
    ? categories.sort((a, b) => b.actualAmount - a.actualAmount).slice(0, 5)
    : categories;
  
  // Cấu hình chung
  const chartConfig = {
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: chartType.value === 'bar' ? 'top' : 'right',
        },
        title: {
          display: true,
          text: 'Chi tiêu theo danh mục: Kế hoạch vs Thực tế'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              let label = context.dataset.label || '';
              if (label) {
                label += ': ';
              }
              label += formatCurrency(context.raw);
              return label;
            }
          }
        }
      }
    }
  };
  
  // Cấu hình riêng cho biểu đồ cột
  if (chartType.value === 'bar') {
    chartConfig.type = 'bar';
    chartConfig.data = {
      labels: topCategories.map(cat => cat.categoryName),
      datasets: [
        {
          label: 'Kế hoạch chi tiêu',
          data: topCategories.map(cat => cat.budgetAmount),
          backgroundColor: 'rgba(239, 68, 68, 0.6)',
          borderColor: 'rgba(239, 68, 68, 1)',
          borderWidth: 1
        },
        {
          label: 'Chi tiêu thực tế',
          data: topCategories.map(cat => cat.actualAmount),
          backgroundColor: 'rgba(245, 158, 11, 0.6)',
          borderColor: 'rgba(245, 158, 11, 1)',
          borderWidth: 1
        }
      ]
    };
    
    chartConfig.options.scales = {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function(value) {
            return formatCurrency(value);
          }
        }
      }
    };
  } 
  // Cấu hình cho biểu đồ tròn/vòng
  else {
    chartConfig.type = chartType.value; // 'pie' hoặc 'doughnut'
    chartConfig.data = {
      labels: topCategories.map(cat => cat.categoryName),
      datasets: [
        {
          label: 'Kế hoạch',
          data: topCategories.map(cat => cat.budgetAmount),
          backgroundColor: [
            'rgba(79, 70, 229, 0.7)',
            'rgba(16, 185, 129, 0.7)',
            'rgba(245, 158, 11, 0.7)',
            'rgba(239, 68, 68, 0.7)',
            'rgba(59, 130, 246, 0.7)',
            'rgba(168, 85, 247, 0.7)',
            'rgba(236, 72, 153, 0.7)',
          ],
          borderWidth: 1,
          hidden: false
        },
        {
          label: 'Thực tế',
          data: topCategories.map(cat => cat.actualAmount),
          backgroundColor: [
            'rgba(79, 70, 229, 0.9)',
            'rgba(16, 185, 129, 0.9)',
            'rgba(245, 158, 11, 0.9)',
            'rgba(239, 68, 68, 0.9)',
            'rgba(59, 130, 246, 0.9)',
            'rgba(168, 85, 247, 0.9)',
            'rgba(236, 72, 153, 0.9)',
          ],
          borderWidth: 1,
          hidden: true
        }
      ]
    };
  }
  
  expenseChart = new Chart(ctx, chartConfig);
  updateChartVisibility('expense');
};

const renderComparisonChart = () => {
  // Kiểm tra lại lần nữa để đảm bảo an toàn
  if (!comparisonChartRef.value) return;
  
  const ctx = comparisonChartRef.value.getContext('2d');
  if (!ctx) return;
  
  comparisonChart = new Chart(ctx, {
    type: chartType.value,
    data: {
      labels: ['Tổng thu nhập', 'Tổng chi phí', 'Lợi nhuận'],
      datasets: [
        {
          label: 'Kế hoạch',
          data: [
            reportData.value.summary.totalIncomeBudget,
            reportData.value.summary.totalExpenseBudget,
            reportData.value.summary.totalIncomeBudget - reportData.value.summary.totalExpenseBudget
          ],
          backgroundColor: 'rgba(79, 70, 229, 0.6)',
          borderColor: 'rgba(79, 70, 229, 1)',
          borderWidth: 1
        },
        {
          label: 'Thực tế',
          data: [
            reportData.value.summary.totalIncomeActual,
            reportData.value.summary.totalExpenseActual,
            reportData.value.summary.totalIncomeActual - reportData.value.summary.totalExpenseActual
          ],
          backgroundColor: 'rgba(16, 185, 129, 0.6)',
          borderColor: 'rgba(16, 185, 129, 1)',
          borderWidth: 1
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'top',
        },
        title: {
          display: true,
          text: 'Tổng quan tài chính: Kế hoạch vs Thực tế'
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              return new Intl.NumberFormat('vi-VN', { 
                style: 'currency', 
                currency: 'VND',
                maximumFractionDigits: 0
              }).format(value);
            }
          }
        }
      }
    }
  });
  updateChartVisibility('comparison');
};

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

// Lifecycle
onMounted(() => {
  // Đảm bảo tất cả DOM đã render trước khi tải dữ liệu
  nextTick(() => {
    loadData();
  });
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo Tháng</template>
    
    <div class="content-container">
      <!-- Filter Section -->
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
              <select v-model="filters.year" class="form-select">
                <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Tháng</label>
              <select v-model="filters.month" class="form-select">
                <option v-for="month in months" :key="month.id" :value="month.id">{{ month.name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Danh mục</label>
              <select v-model="filters.categoryId" class="form-select">
                <option :value="null">Tất cả danh mục</option>
                <option v-for="category in incomeCategories" :key="'income-'+category.categoryId" :value="{type: 'income', id: category.categoryId}">
                  Thu nhập: {{ category.categoryName }}
                </option>
                <option v-for="category in expenseCategories" :key="'expense-'+category.categoryId" :value="{type: 'expense', id: category.categoryId}">
                  Chi tiêu: {{ category.categoryName }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Loại biểu đồ</label>
              <select v-model="chartType" class="form-select">
                <option value="bar">Biểu đồ cột</option>
                <option value="pie">Biểu đồ tròn</option>
                <option value="doughnut">Biểu đồ vòng</option>
              </select>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="applyFilter" class="btn-primary">
              <i class="bi bi-search"></i> Xem báo cáo
            </button>
          </div>
        </div>
      </div>
      
      <!-- Report Content -->
      <div v-if="isLoading" class="card-empty-state">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu báo cáo...</p>
      </div>
      
      <div v-else-if="error" class="card-empty-state error">
        <i class="bi bi-exclamation-circle error-icon"></i>
        <p>{{ error }}</p>
        <button @click="loadData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <div v-else-if="!reportData" class="card-empty-state">
        <i class="bi bi-bar-chart empty-icon"></i>
        <p>Không có dữ liệu báo cáo</p>
      </div>
      
      <template v-else>
        <!-- Summary Cards -->
        <div class="summary-cards">
          <div class="summary-card income">
            <div class="summary-icon">
              <i class="bi bi-graph-up-arrow"></i>
            </div>
            <div class="summary-content">
              <h3 class="summary-title">Tổng thu nhập</h3>
              <div class="summary-amounts">
                <div class="amount-item">
                  <span class="amount-label">Kế hoạch:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalIncomeBudget) }}</span>
                </div>
                <div class="amount-item">
                  <span class="amount-label">Thực tế:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalIncomeActual) }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="summary-card expense">
            <div class="summary-icon">
              <i class="bi bi-graph-down-arrow"></i>
            </div>
            <div class="summary-content">
              <h3 class="summary-title">Tổng chi phí</h3>
              <div class="summary-amounts">
                <div class="amount-item">
                  <span class="amount-label">Kế hoạch:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalExpenseBudget) }}</span>
                </div>
                <div class="amount-item">
                  <span class="amount-label">Thực tế:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalExpenseActual) }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="summary-card profit">
            <div class="summary-icon">
              <i class="bi bi-piggy-bank"></i>
            </div>
            <div class="summary-content">
              <h3 class="summary-title">Lợi nhuận</h3>
              <div class="summary-amounts">
                <div class="amount-item">
                  <span class="amount-label">Kế hoạch:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalIncomeBudget - reportData.summary.totalExpenseBudget) }}</span>
                </div>
                <div class="amount-item">
                  <span class="amount-label">Thực tế:</span>
                  <span class="amount-value">{{ formatCurrency(reportData.summary.totalIncomeActual - reportData.summary.totalExpenseActual) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Charts -->
        <div class="charts-container">
          <!-- Comparison Chart -->
          <div class="chart-card full-width">
            <div class="chart-header">
              <h3 class="chart-title">Tổng quan tài chính</h3>
              <div class="chart-actions">
                <button @click="toggleDataset('comparison', 0)" class="btn-pill">Toggle Kế hoạch</button>
                <button @click="toggleDataset('comparison', 1)" class="btn-pill">Toggle Thực tế</button>
              </div>
            </div>
            <div v-show="reportData && !isLoading" class="chart-body">
              <canvas ref="comparisonChartRef" height="250"></canvas>
            </div>
          </div>
          
          <!-- Income Chart -->
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">Thu nhập theo danh mục</h3>
              <div class="chart-actions">
                <button @click="toggleDataset('income', 0)" class="btn-pill">Toggle Kế hoạch</button>
                <button @click="toggleDataset('income', 1)" class="btn-pill">Toggle Thực tế</button>
              </div>
            </div>
            <div v-show="reportData && !isLoading" class="chart-body">
              <canvas ref="incomeChartRef" height="250"></canvas>
            </div>
          </div>
          
          <!-- Expense Chart -->
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">Chi tiêu theo danh mục</h3>
              <div class="chart-actions">
                <button @click="toggleDataset('expense', 0)" class="btn-pill">Toggle Kế hoạch</button>
                <button @click="toggleDataset('expense', 1)" class="btn-pill">Toggle Thực tế</button>
              </div>
            </div>
            <div v-show="reportData && !isLoading" class="chart-body">
              <canvas ref="expenseChartRef" height="250"></canvas>
            </div>
          </div>
        </div>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-container {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.filter-container {
  margin-bottom: 2rem;
}

.filter-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.filter-header .card-title {
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.filter-header .card-title i {
  margin-right: 0.5rem;
}

.filter-content {
  display: flex;
  flex-direction: column;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary i {
  margin-right: 0.5rem;
}

.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background-color: #f9fafb;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  text-align: center;
}

.card-empty-state.error {
  background-color: #fee2e2;
  border-color: #fca5a5;
}

.card-empty-state .loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 4px solid #d1d5db;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

.card-empty-state .error-icon {
  font-size: 2rem;
  color: #ef4444;
  margin-bottom: 1rem;
}

.card-empty-state .empty-icon {
  font-size: 2rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  background-color: #f9fafb;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.summary-card .summary-icon {
  font-size: 2rem;
  margin-right: 1rem;
}

.summary-card.income .summary-icon {
  color: #16a34a;
}

.summary-card.expense .summary-icon {
  color: #ef4444;
}

.summary-card.profit .summary-icon {
  color: #f59e0b;
}

.summary-card .summary-content {
  flex: 1;
}

.summary-card .summary-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.summary-card .summary-amounts {
  display: flex;
  flex-direction: column;
}

.summary-card .amount-item {
  display: flex;
  justify-content: space-between;
}

.summary-card .amount-label {
  font-weight: 500;
}

.summary-card .amount-value {
  font-weight: 600;
}

.charts-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 1024px) {
  .charts-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

.chart-card {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.chart-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-pill {
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-pill.active {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

@media (min-width: 1024px) {
  .charts-container {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-card.full-width {
    grid-column: 1 / -1;
  }
}

.chart-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
}

.chart-body {
  padding: 1.5rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
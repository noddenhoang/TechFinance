<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';
import AppLayout from '../components/layouts/AppLayout.vue';
import { financialReports } from '../api/financialReports';
import Chart from 'chart.js/auto';

const authStore = useAuthStore();
const router = useRouter();

// Trạng thái dữ liệu
const monthlyReportData = ref(null);
const yearlyTrendData = ref(null);
const isLoading = ref(true);
const error = ref(null);
const currentYear = new Date().getFullYear();
const currentMonth = new Date().getMonth() + 1;

// Tham chiếu biểu đồ
const summaryChartRef = ref(null);
const categoryChartRef = ref(null);
const trendChartRef = ref(null);
const yearlyComparisonChartRef = ref(null);

// Biểu đồ instances
let summaryChart = null;
let categoryChart = null;
let trendChart = null;
let yearlyComparisonChart = null;

// Danh sách tháng
const months = [
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
];

// Thêm vào phần đầu, sau khi định nghĩa months
const shortMonthNames = [
  'T1', 'T2', 'T3', 'T4', 'T5', 'T6', 
  'T7', 'T8', 'T9', 'T10', 'T11', 'T12'
];

// KPI Metrics
const kpiMetrics = computed(() => {
  if (!monthlyReportData.value) return null;
  
  const summary = monthlyReportData.value.summary;
  
  return {
    incomeBudget: summary.totalIncomeBudget,
    incomeActual: summary.totalIncomeActual,
    expenseBudget: summary.totalExpenseBudget,
    expenseActual: summary.totalExpenseActual,
    profit: summary.totalIncomeActual - summary.totalExpenseActual,
    planProfit: summary.totalIncomeBudget - summary.totalExpenseBudget,
    
    // Tính toán % so với kế hoạch
    incomePercentage: summary.totalIncomeBudget > 0 
      ? (summary.totalIncomeActual / summary.totalIncomeBudget) * 100 
      : 0,
    expensePercentage: summary.totalExpenseBudget > 0 
      ? (summary.totalExpenseActual / summary.totalExpenseBudget) * 100 
      : 0,
    profitPercentage: (summary.totalIncomeBudget - summary.totalExpenseBudget) > 0
      ? ((summary.totalIncomeActual - summary.totalExpenseActual) / (summary.totalIncomeBudget - summary.totalExpenseBudget)) * 100
      : 0
  };
});

// Tổng hợp danh mục
const topIncomeCategories = computed(() => {
  if (!monthlyReportData.value?.incomeCategories) return [];
  return [...monthlyReportData.value.incomeCategories]
    .sort((a, b) => b.actualAmount - a.actualAmount)
    .slice(0, 5);
});

const topExpenseCategories = computed(() => {
  if (!monthlyReportData.value?.expenseCategories) return [];
  return [...monthlyReportData.value.expenseCategories]
    .sort((a, b) => b.actualAmount - a.actualAmount)
    .slice(0, 5);
});

// Tải dữ liệu Dashboard
const loadDashboardData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Tải báo cáo tháng hiện tại
    monthlyReportData.value = await financialReports.getMonthlyReport(currentYear, currentMonth);
    
    // Tải dữ liệu cho biểu đồ xu hướng theo năm
    yearlyTrendData.value = await financialReports.getYearlyReport(currentYear);
    
    await nextTick();
    renderCharts();
  } catch (err) {
    console.error('Error loading dashboard data:', err);
    error.value = 'Không thể tải dữ liệu dashboard. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Render tất cả biểu đồ
const renderCharts = () => {
  // Xóa biểu đồ hiện tại nếu có
  if (summaryChart) summaryChart.destroy();
  if (categoryChart) categoryChart.destroy();
  if (trendChart) trendChart.destroy();
  if (yearlyComparisonChart) yearlyComparisonChart.destroy();
  
  // Đảm bảo có dữ liệu trước khi vẽ biểu đồ
  if (!monthlyReportData.value || !yearlyTrendData.value) return;
  
  // Đợi đủ thời gian để DOM được render
  setTimeout(() => {
    renderSummaryChart();
    renderCategoryChart();
    renderTrendChart();
    renderYearlyComparisonChart();
  }, 300);
};

// Biểu đồ tổng quan tài chính
const renderSummaryChart = () => {
  if (!summaryChartRef.value) return;
  
  const ctx = summaryChartRef.value.getContext('2d');
  if (!ctx) return;
  
  summaryChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Thu nhập', 'Chi tiêu', 'Lợi nhuận'],
      datasets: [
        {
          label: 'Kế hoạch',
          data: [
            monthlyReportData.value.summary.totalIncomeBudget,
            monthlyReportData.value.summary.totalExpenseBudget,
            monthlyReportData.value.summary.totalIncomeBudget - monthlyReportData.value.summary.totalExpenseBudget
          ],
          backgroundColor: 'rgba(79, 70, 229, 0.6)',
          borderColor: 'rgba(79, 70, 229, 1)',
          borderWidth: 1
        },
        {
          label: 'Thực tế',
          data: [
            monthlyReportData.value.summary.totalIncomeActual,
            monthlyReportData.value.summary.totalExpenseActual,
            monthlyReportData.value.summary.totalIncomeActual - monthlyReportData.value.summary.totalExpenseActual
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
          text: `Tổng quan tài chính - ${months[currentMonth-1].name} ${currentYear}`
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
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              return formatCurrency(value);
            }
          }
        }
      }
    }
  });
};

// Biểu đồ danh mục hàng đầu
const renderCategoryChart = () => {
  if (!categoryChartRef.value) return;
  
  const ctx = categoryChartRef.value.getContext('2d');
  if (!ctx) return;
  
  // Kết hợp 5 danh mục thu nhập hàng đầu và 5 danh mục chi tiêu hàng đầu
  const categories = [
    ...topIncomeCategories.value.map(cat => ({ 
      name: `Thu: ${cat.categoryName}`, 
      value: cat.actualAmount,
      type: 'income'
    })),
    ...topExpenseCategories.value.map(cat => ({ 
      name: `Chi: ${cat.categoryName}`, 
      value: cat.actualAmount,
      type: 'expense'
    }))
  ].sort((a, b) => b.value - a.value).slice(0, 10);
  
  categoryChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: categories.map(cat => cat.name),
      datasets: [{
        data: categories.map(cat => Math.abs(cat.value)),
        backgroundColor: categories.map(cat => 
          cat.type === 'income' 
            ? 'rgba(16, 185, 129, 0.7)' 
            : 'rgba(239, 68, 68, 0.7)'),
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'right',
        },
        title: {
          display: true,
          text: 'Top 10 danh mục'
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              let label = context.label || '';
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
  });
};

// Biểu đồ xu hướng theo tháng
const renderTrendChart = () => {
  if (!trendChartRef.value || !yearlyTrendData.value) return;
  
  const ctx = trendChartRef.value.getContext('2d');
  if (!ctx) return;
  
  // Chuẩn bị dữ liệu - sử dụng tên tháng rút gọn
  const monthLabels = shortMonthNames;
  
  // Dữ liệu thu nhập và chi tiêu theo tháng
  const incomeData = Array(12).fill(0);
  const expenseData = Array(12).fill(0);
  const profitData = Array(12).fill(0);
  
  // Điền dữ liệu từ báo cáo năm (theo tháng)
  yearlyTrendData.value.forEach(report => {
    const monthIndex = report.month - 1;
    incomeData[monthIndex] = report.summary.totalIncomeActual;
    expenseData[monthIndex] = report.summary.totalExpenseActual;
    profitData[monthIndex] = report.summary.totalIncomeActual - report.summary.totalExpenseActual;
  });
  
  trendChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: monthLabels,
      datasets: [
        {
          label: 'Thu nhập',
          data: incomeData,
          borderColor: 'rgba(16, 185, 129, 1)',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          tension: 0.3,
          fill: false
        },
        {
          label: 'Chi tiêu',
          data: expenseData,
          borderColor: 'rgba(239, 68, 68, 1)',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          tension: 0.3,
          fill: false
        },
        {
          label: 'Lợi nhuận',
          data: profitData,
          borderColor: 'rgba(79, 70, 229, 1)',
          backgroundColor: 'rgba(79, 70, 229, 0.1)',
          tension: 0.3,
          fill: false
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
          text: `Xu hướng tài chính năm ${currentYear}`
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
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              return formatCurrency(value);
            }
          }
        }
      }
    }
  });
};

// Biểu đồ so sánh thu/chi theo quý
const renderYearlyComparisonChart = () => {
  if (!yearlyComparisonChartRef.value || !yearlyTrendData.value) return;
  
  const ctx = yearlyComparisonChartRef.value.getContext('2d');
  if (!ctx) return;
  
  // Tính tổng thu nhập và chi tiêu theo quý
  const quarterlyData = [
    { income: 0, expense: 0, profit: 0 }, // Q1
    { income: 0, expense: 0, profit: 0 }, // Q2
    { income: 0, expense: 0, profit: 0 }, // Q3
    { income: 0, expense: 0, profit: 0 }  // Q4
  ];
  
  yearlyTrendData.value.forEach(report => {
    const quarterIndex = Math.floor((report.month - 1) / 3);
    quarterlyData[quarterIndex].income += report.summary.totalIncomeActual;
    quarterlyData[quarterIndex].expense += report.summary.totalExpenseActual;
    quarterlyData[quarterIndex].profit += (report.summary.totalIncomeActual - report.summary.totalExpenseActual);
  });
  
  yearlyComparisonChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Quý 1', 'Quý 2', 'Quý 3', 'Quý 4'],
      datasets: [
        {
          label: 'Thu nhập',
          data: quarterlyData.map(q => q.income),
          backgroundColor: 'rgba(16, 185, 129, 0.7)',
          borderColor: 'rgba(16, 185, 129, 1)',
          borderWidth: 1
        },
        {
          label: 'Chi tiêu',
          data: quarterlyData.map(q => q.expense),
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: 'rgba(239, 68, 68, 1)',
          borderWidth: 1
        },
        {
          label: 'Lợi nhuận',
          data: quarterlyData.map(q => q.profit),
          backgroundColor: 'rgba(79, 70, 229, 0.7)',
          borderColor: 'rgba(79, 70, 229, 1)',
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
          text: `So sánh theo quý năm ${currentYear}`
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
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              return formatCurrency(value);
            }
          }
        }
      }
    }
  });
};

// Định dạng tiền tệ
const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

// Format phần trăm
const formatPercentage = (value) => {
  return value.toFixed(1) + '%';
};

// Xác định class dựa trên giá trị KPI
const getKpiClass = (value) => {
  if (value > 0) return 'positive';
  if (value < 0) return 'negative';
  return '';
};

// Xác định icon dựa trên giá trị KPI
const getKpiIcon = (value) => {
  if (value > 0) return 'bi-arrow-up-right';
  if (value < 0) return 'bi-arrow-down-right';
  return 'bi-dash';
};

// Lifecycle
onMounted(() => {
  // Đảm bảo DOM đã render trước khi tải dữ liệu
  nextTick(() => {
    loadDashboardData();
  });
});
</script>

<template>
  <AppLayout>
    <template #page-title>Dashboard</template>
    
    <div class="dashboard-container">
      <!-- Loading State -->
      <div v-if="isLoading" class="card-empty-state">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu dashboard...</p>
      </div>
      
      <!-- Error State -->
      <div v-else-if="error" class="card-empty-state error">
        <i class="bi bi-exclamation-circle error-icon"></i>
        <p>{{ error }}</p>
        <button @click="loadDashboardData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <!-- Dashboard Content -->
      <template v-else-if="monthlyReportData">
        <!-- Welcome Section -->
        <div class="dashboard-welcome">
          <h2>Chào mừng, {{ authStore.user?.fullName || 'Người dùng' }}!</h2>
          <p class="subtitle">Dashboard tổng quan tài chính {{ months[currentMonth-1].name }} {{ currentYear }}</p>
        </div>
        
        <!-- KPI Cards Row -->
        <div class="kpi-cards">
          <!-- Income KPI -->
          <div class="kpi-card income">
            <div class="kpi-header">
              <span class="kpi-title">Thu nhập</span>
              <i class="bi bi-graph-up-arrow"></i>
            </div>
            <div class="kpi-value">{{ formatCurrency(kpiMetrics.incomeActual) }}</div>
            <div 
              class="kpi-comparison" 
              :class="getKpiClass(kpiMetrics.incomeActual - kpiMetrics.incomeBudget)"
            >
              <i :class="['bi', getKpiIcon(kpiMetrics.incomeActual - kpiMetrics.incomeBudget)]"></i>
              {{ formatCurrency(Math.abs(kpiMetrics.incomeActual - kpiMetrics.incomeBudget)) }}
              so với kế hoạch
            </div>
            <div class="kpi-progress">
              <div class="progress-bar">
                <div 
                  class="progress-fill" 
                  :style="{ width: `${Math.min(kpiMetrics.incomePercentage, 100)}%` }"
                ></div>
              </div>
              <span class="progress-text">{{ formatPercentage(kpiMetrics.incomePercentage) }}</span>
            </div>
          </div>
          
          <!-- Expense KPI -->
          <div class="kpi-card expense">
            <div class="kpi-header">
              <span class="kpi-title">Chi tiêu</span>
              <i class="bi bi-graph-down-arrow"></i>
            </div>
            <div class="kpi-value">{{ formatCurrency(kpiMetrics.expenseActual) }}</div>
            <div 
              class="kpi-comparison" 
              :class="getKpiClass(kpiMetrics.expenseBudget - kpiMetrics.expenseActual)"
            >
              <i :class="['bi', getKpiIcon(kpiMetrics.expenseBudget - kpiMetrics.expenseActual)]"></i>
              {{ formatCurrency(Math.abs(kpiMetrics.expenseActual - kpiMetrics.expenseBudget)) }}
              so với kế hoạch
            </div>
            <div class="kpi-progress">
              <div class="progress-bar">
                <div 
                  class="progress-fill" 
                  :style="{ width: `${Math.min(kpiMetrics.expensePercentage, 100)}%` }"
                ></div>
              </div>
              <span class="progress-text">{{ formatPercentage(kpiMetrics.expensePercentage) }}</span>
            </div>
          </div>
          
          <!-- Profit KPI -->
          <div class="kpi-card profit">
            <div class="kpi-header">
              <span class="kpi-title">Lợi nhuận</span>
              <i class="bi bi-piggy-bank"></i>
            </div>
            <div class="kpi-value">{{ formatCurrency(kpiMetrics.profit) }}</div>
            <div 
              class="kpi-comparison" 
              :class="getKpiClass(kpiMetrics.profit - kpiMetrics.planProfit)"
            >
              <i :class="['bi', getKpiIcon(kpiMetrics.profit - kpiMetrics.planProfit)]"></i>
              {{ formatCurrency(Math.abs(kpiMetrics.profit - kpiMetrics.planProfit)) }}
              so với kế hoạch
            </div>
            <div class="kpi-progress">
              <div class="progress-bar">
                <div 
                  class="progress-fill" 
                  :style="{ width: `${Math.min(kpiMetrics.profitPercentage, 100)}%` }"
                ></div>
              </div>
              <span class="progress-text">{{ formatPercentage(kpiMetrics.profitPercentage) }}</span>
            </div>
          </div>
        </div>
        
        <!-- Chart Rows -->
        <div class="chart-grid">
          <!-- Summary Chart -->
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">Tổng quan tài chính tháng này</h3>
            </div>
            <div class="chart-body">
              <canvas ref="summaryChartRef" height="250"></canvas>
            </div>
          </div>
          
          <!-- Categories Chart -->
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">Danh mục hàng đầu</h3>
            </div>
            <div class="chart-body">
              <canvas ref="categoryChartRef" height="250"></canvas>
            </div>
          </div>
          
          <!-- Trend Chart (Full Width) -->
          <div class="chart-card full-width">
            <div class="chart-header">
              <h3 class="chart-title">Xu hướng tài chính năm {{ currentYear }}</h3>
            </div>
            <div class="chart-body">
              <canvas ref="trendChartRef" height="250"></canvas>
            </div>
          </div>
          
          <!-- Quarterly Comparison Chart (Full Width) -->
          <div class="chart-card full-width">
            <div class="chart-header">
              <h3 class="chart-title">So sánh theo quý</h3>
            </div>
            <div class="chart-body">
              <canvas ref="yearlyComparisonChartRef" height="250"></canvas>
            </div>
          </div>
        </div>
        
        <!-- Quick Actions -->
        <div class="quick-actions">
          <h3 class="section-title">Truy cập nhanh</h3>
          <div class="actions-grid">
            <router-link to="/income-transactions" class="action-card">
              <div class="action-icon income">
                <i class="bi bi-cash-coin"></i>
              </div>
              <span class="action-title">Thu nhập</span>
              <span class="action-desc">Quản lý giao dịch thu nhập</span>
            </router-link>
            
            <router-link to="/expense-transactions" class="action-card">
              <div class="action-icon expense">
                <i class="bi bi-credit-card"></i>
              </div>
              <span class="action-title">Chi tiêu</span>
              <span class="action-desc">Quản lý giao dịch chi tiêu</span>
            </router-link>
            
            <router-link to="/monthly-reports" class="action-card">
              <div class="action-icon">
                <i class="bi bi-bar-chart"></i>
              </div>
              <span class="action-title">Báo cáo</span>
              <span class="action-desc">Xem báo cáo tài chính</span>
            </router-link>
            
            <router-link to="/income-budgets" class="action-card">
              <div class="action-icon">
                <i class="bi bi-calculator"></i>
              </div>
              <span class="action-title">Ngân sách</span>
              <span class="action-desc">Quản lý ngân sách</span>
            </router-link>
          </div>
        </div>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.dashboard-container {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.dashboard-welcome {
  margin-bottom: 2rem;
}

.dashboard-welcome h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 0.5rem;
  font-size: 1.5rem;
  font-weight: 600;
}

.dashboard-welcome .subtitle {
  color: #6b7280;
  margin: 0;
  font-size: 1.1rem;
}

/* KPI Cards */
.kpi-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.kpi-card {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  border-top: 4px solid transparent;
}

.kpi-card.income {
  border-top-color: #10b981;
}

.kpi-card.expense {
  border-top-color: #ef4444;
}

.kpi-card.profit {
  border-top-color: #f59e0b;
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.kpi-title {
  font-size: 1rem;
  font-weight: 600;
  color: #4b5563;
}

.kpi-header i {
  font-size: 1.25rem;
}

.kpi-card.income .kpi-header i {
  color: #10b981;
}

.kpi-card.expense .kpi-header i {
  color: #ef4444;
}

.kpi-card.profit .kpi-header i {
  color: #f59e0b;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.5rem;
}

.kpi-comparison {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  margin-bottom: 1rem;
  color: #6b7280;
}

.kpi-comparison.positive {
  color: #10b981;
}

.kpi-comparison.negative {
  color: #ef4444;
}

.kpi-progress {
  display: flex;
  align-items: center;
}

.progress-bar {
  flex-grow: 1;
  height: 0.5rem;
  background-color: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
  margin-right: 0.5rem;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background-color: #3b82f6;
}

.kpi-card.income .progress-fill {
  background-color: #10b981;
}

.kpi-card.expense .progress-fill {
  background-color: #ef4444;
}

.kpi-card.profit .progress-fill {
  background-color: #f59e0b;
}

.progress-text {
  font-size: 0.875rem;
  font-weight: 500;
  color: #6b7280;
  width: 3.5rem;
  text-align: right;
}

/* Chart Grid */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.chart-card {
  background-color: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.chart-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
}

.chart-body {
  padding: 1.5rem;
}

/* Quick Actions */
.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin-top: 0;
  margin-bottom: 1rem;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.action-card {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  background-color: #dbeafe;
  color: #3b82f6;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  font-size: 1.25rem;
}

.action-icon.income {
  background-color: #d1fae5;
  color: #10b981;
}

.action-icon.expense {
  background-color: #fee2e2;
  color: #ef4444;
}

.action-title {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.5rem;
}

.action-desc {
  font-size: 0.875rem;
  color: #6b7280;
}

/* Loading and Error States */
.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  text-align: center;
}

.loading-spinner {
  width: 2.5rem;
  height: 2.5rem;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

.error-icon {
  font-size: 2.5rem;
  color: #ef4444;
  margin-bottom: 1rem;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  border: none;
  font-size: 0.875rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Responsive Adjustments */
@media (max-width: 1024px) {
  .kpi-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-grid {
    grid-template-columns: 1fr;
  }
  
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .kpi-cards {
    grid-template-columns: 1fr;
  }
  
  .actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { financialReports } from '../../api/financialReports';

// Simplified filters - only report type
const filters = reactive({
  year: new Date().getFullYear(),
  reportType: 'income' // 'income' or 'expense'
});

// Current year for filter selection
const currentYear = new Date().getFullYear();

// Change year and reload data
const changeYear = (year) => {
  filters.year = year;
  loadData();
};

// Data state
const reportData = ref(null);
const isLoading = ref(false);
const error = ref(null);

// UI State
const activeTab = ref('month'); // 'month' or 'quarter'

// Load report data - always load yearly data
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Always load yearly data for the enhanced UI
      reportData.value = await financialReports.getYearlyReport(filters.year);
  } catch (err) {
    console.error('Error loading report data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Calculate total amount by summing all category amounts
const calculateTotalAmount = () => {
  if (!categories.value || !categories.value.length) {
    return 0;
  }
  
  return categories.value.reduce((total, category) => {
    return total + (category.actualAmount || 0);
  }, 0);
};

// Get monthly amount for a specific category and month
const getCategoryMonthlyAmount = (category, month) => {
  if (!reportData.value || !Array.isArray(reportData.value)) return formatCurrency(0);
  
  // Find the month data in the report
  const monthData = reportData.value.find(m => m.month === month && m.year === filters.year);
  if (!monthData) return formatCurrency(0);
  
  // Find the category in that month
  const categoryList = filters.reportType === 'income' ? monthData.incomeCategories : monthData.expenseCategories;
  const categoryData = categoryList.find(c => c.categoryId === category.categoryId);
  
  return formatCurrency(categoryData ? categoryData.actualAmount : 0);
};

// Get quarterly amount for a specific category and quarter
const getCategoryQuarterlyAmount = (category, quarter) => {
  if (!reportData.value || !Array.isArray(reportData.value)) return formatCurrency(0);
  
  // Calculate months in the quarter
  const startMonth = (quarter - 1) * 3 + 1;
  const endMonth = startMonth + 2;
  
  // Sum all amounts for this category in those months
  let total = 0;
  for (let month = startMonth; month <= endMonth; month++) {
    const monthData = reportData.value.find(m => m.month === month && m.year === filters.year);
    if (!monthData) continue;
    
    const categoryList = filters.reportType === 'income' ? monthData.incomeCategories : monthData.expenseCategories;
    const categoryData = categoryList.find(c => c.categoryId === category.categoryId);
    
    if (categoryData) {
      total += parseFloat(categoryData.actualAmount || 0);
    }
  }
  
  return formatCurrency(total);
};

// Get total amount for a specific month across all categories
const getMonthlyTotal = (month) => {
  if (!reportData.value || !Array.isArray(reportData.value)) return formatCurrency(0);
  
  const monthData = reportData.value.find(m => m.month === month && m.year === filters.year);
  if (!monthData) return formatCurrency(0);
  
  const total = filters.reportType === 'income' 
    ? monthData.summary.totalIncomeActual 
    : monthData.summary.totalExpenseActual;
    
  return formatCurrency(total);
};

// Get total amount for a specific quarter across all categories
const getQuarterlyTotal = (quarter) => {
  if (!reportData.value || !Array.isArray(reportData.value)) return formatCurrency(0);
  
  // Calculate months in the quarter
  const startMonth = (quarter - 1) * 3 + 1;
  const endMonth = startMonth + 2;
  
  // Sum all totals for these months
  let total = 0;
  for (let month = startMonth; month <= endMonth; month++) {
    const monthData = reportData.value.find(m => m.month === month && m.year === filters.year);
    if (!monthData) continue;
    
    total += parseFloat(filters.reportType === 'income' 
      ? monthData.summary.totalIncomeActual 
      : monthData.summary.totalExpenseActual);
  }
  
  return formatCurrency(total);
};

// Lấy danh sách danh mục
const categories = computed(() => {
  if (!reportData.value) return [];
  
  // Nếu là báo cáo năm, ghép danh mục từ các tháng
  // Thống kê theo danh mục
  const categoryMap = new Map();
  
  reportData.value.forEach(month => {
    const monthCategories = filters.reportType === 'income' 
      ? month.incomeCategories 
      : month.expenseCategories;
    
    monthCategories.forEach(category => {
      if (!categoryMap.has(category.categoryId)) {
        categoryMap.set(category.categoryId, {
          categoryId: category.categoryId,
          categoryName: category.categoryName,
          budgetAmount: 0,
          actualAmount: 0,
          difference: 0,
          percentageOfTotal: 0
        });
      }
      
      const existingCategory = categoryMap.get(category.categoryId);
      existingCategory.budgetAmount += category.budgetAmount;
      existingCategory.actualAmount += category.actualAmount;
      existingCategory.difference += category.difference;
    });
  });
  
  // Tính lại phần trăm của tổng
  const totalActual = calculateTotalAmount();
  const categoriesArray = Array.from(categoryMap.values());
  
  categoriesArray.forEach(category => {
    if (totalActual > 0) {
      category.percentageOfTotal = (category.actualAmount / totalActual) * 100;
    } else {
      category.percentageOfTotal = 0;
    }
  });
  
  // Sắp xếp theo số tiền thực tế giảm dần
  return categoriesArray.sort((a, b) => b.actualAmount - a.actualAmount);
});

const formatCurrency = (value) => {
  if (!value) return '0 đ';
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

const formatPercentage = (value) => {
  if (!value) return '0%';
  return `${Math.round(value)}%`;
};

// Change the years computed property to return years from 2020 to current
const years = computed(() => {
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

// Lifecycle
onMounted(() => {
  // Đảm bảo DOM đã render trước khi tải dữ liệu
  nextTick(() => {
    loadData();
  });
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo Theo Danh mục</template>
    
    <div class="content-container">
      <h2>Báo cáo chi tiết theo danh mục</h2>
      <p>Trang này hiển thị báo cáo thu nhập và chi phí theo từng danh mục.</p>
      
      <!-- Simplified filter container -->
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
              <select v-model="filters.year" class="form-select" @change="loadData">
                <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Loại báo cáo</label>
              <select v-model="filters.reportType" class="form-select" @change="loadData">
                <option value="income">8.4. Thu nhập theo danh mục</option>
                <option value="expense">8.5. Chi phí theo danh mục</option>
              </select>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Report Container -->
      <div class="report-container" v-if="!isLoading && !error && reportData">
        <!-- Report header -->
        <div class="report-header">
          <h2 class="report-title">
            {{ filters.reportType === 'income' ? 'Báo cáo Thu Nhập Theo Danh Mục' : 'Báo cáo Chi Phí Theo Danh Mục' }}
          </h2>
        </div>
        
        <!-- Tab navigation -->
        <div class="report-tabs">
          <div class="tab-container">
            <button 
              class="tab-button" 
              :class="{ active: activeTab === 'month' }"
              @click="activeTab = 'month'"
            >
              Tháng
            </button>
            <button 
              class="tab-button" 
              :class="{ active: activeTab === 'quarter' }"
              @click="activeTab = 'quarter'"
            >
              Quý
            </button>
          </div>
        </div>
        
        <!-- Monthly view table -->
        <div v-if="activeTab === 'month'" class="report-table-container">
          <table class="report-table">
              <thead>
                <tr>
                <th>Danh mục</th>
                <th v-for="month in 12" :key="month">T{{ month }}</th>
                <th>Tổng năm</th>
                <th>%</th>
                </tr>
              </thead>
              <tbody>
              <tr v-for="category in categories" :key="category.categoryId">
                <td>{{ category.categoryName }}</td>
                <!-- Monthly amounts -->
                <template v-for="month in 12" :key="month">
                  <td>
                    {{ getCategoryMonthlyAmount(category, month) }}
                  </td>
                </template>
                <td>{{ formatCurrency(category.actualAmount) }}</td>
                <td>{{ formatPercentage(category.percentageOfTotal) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>Tổng cộng</th>
                <!-- Monthly totals -->
                <template v-for="month in 12" :key="month">
                  <th>
                    {{ getMonthlyTotal(month) }}
                  </th>
                </template>
                <th>{{ formatCurrency(calculateTotalAmount()) }}</th>
                <th>100%</th>
                </tr>
              </tfoot>
            </table>
          </div>
          
        <!-- Quarterly view table -->
        <div v-if="activeTab === 'quarter'" class="report-table-container">
          <table class="report-table">
                  <thead>
                    <tr>
                      <th>Danh mục</th>
                <th>Q1</th>
                <th>Q2</th>
                <th>Q3</th>
                <th>Q4</th>
                <th>Tổng năm</th>
                <th>%</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="category in categories" :key="category.categoryId">
                <td>{{ category.categoryName }}</td>
                <!-- Quarterly amounts -->
                <template v-for="quarter in 4" :key="quarter">
                  <td>
                    {{ getCategoryQuarterlyAmount(category, quarter) }}
                  </td>
                </template>
                <td>{{ formatCurrency(category.actualAmount) }}</td>
                <td>{{ formatPercentage(category.percentageOfTotal) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>Tổng cộng</th>
                <!-- Quarterly totals -->
                <template v-for="quarter in 4" :key="quarter">
                  <th>
                    {{ getQuarterlyTotal(quarter) }}
                  </th>
                </template>
                <th>{{ formatCurrency(calculateTotalAmount()) }}</th>
                <th>100%</th>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      
      <!-- Loading indicator -->
      <div v-if="isLoading" class="loading-spinner-container">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu báo cáo...</p>
      </div>
      
      <!-- Error message -->
      <div v-else-if="error" class="error-message">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
        <button @click="loadData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <!-- No data message -->
      <div v-else-if="!reportData" class="no-data">
        <i class="bi bi-info-circle"></i>
        <p>Không có dữ liệu báo cáo.</p>
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

/* Report styles */
.report-container {
  margin-top: 2rem;
}

.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.report-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
}

.report-actions {
  display: flex;
  align-items: center;
}

.filter-group {
  margin-right: 1rem;
}

.year-selector {
  display: flex;
  gap: 0.25rem;
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

.button-group {
  display: flex;
  gap: 0.5rem;
}

.report-tabs {
  margin-bottom: 1rem;
}

.tab-container {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
}

.tab-button {
  padding: 0.75rem 1.5rem;
  background-color: transparent;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-weight: 500;
  transition: all 0.2s;
}

.tab-button:hover {
  color: #4b5563;
}

.tab-button.active {
  color: #2563eb;
  border-bottom: 2px solid #2563eb;
}

/* Report table styles */
.report-table-container {
  overflow-x: auto;
  margin-bottom: 2rem;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.report-table th,
.report-table td {
  border: 1px solid #e5e7eb;
  padding: 0.5rem 0.75rem;
  text-align: left;
}

.report-table th {
  background-color: #f3f4f6;
  font-weight: 600;
  color: #374151;
}

.report-table tbody tr:nth-child(even) {
  background-color: #f9fafb;
}

.report-table tbody tr:hover {
  background-color: #f3f4f6;
}

.report-table tfoot {
  font-weight: 600;
}

/* Loading, error, and no data states */
.loading-spinner-container,
.error-message,
.no-data {
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

.error-message i,
.no-data i {
  font-size: 2rem;
  margin-bottom: 1rem;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
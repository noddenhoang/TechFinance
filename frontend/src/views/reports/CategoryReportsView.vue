<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { financialReports } from '../../api/financialReports';

// Thay đổi cấu trúc filters
const filters = reactive({
  year: new Date().getFullYear(),
  timeFrame: 'month', // 'month', 'quarter', 'year'
  month: new Date().getMonth() + 1,
  quarter: Math.floor(new Date().getMonth() / 3) + 1,
  reportType: 'income' // 'income' hoặc 'expense'
});

// Data state
const reportData = ref(null);
const isLoading = ref(false);
const error = ref(null);

// Thêm state để kiểm soát hiển thị chi tiết từng tháng
const showMonthlyDetails = ref(false);

// Thêm phương thức để toggle hiển thị chi tiết
const toggleMonthlyDetails = () => {
  showMonthlyDetails.value = !showMonthlyDetails.value;
};

// Thêm state quản lý hiện tại đang xem tháng nào
const activeMonths = reactive(new Set());

// Phương thức để toggle hiển thị chi tiết tháng cụ thể
const toggleMonthDetail = (monthId) => {
  if (activeMonths.has(monthId)) {
    activeMonths.delete(monthId);
  } else {
    activeMonths.add(monthId);
  }
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

// Thêm danh sách quý
const quarters = computed(() => [
  { id: 1, name: 'Quý 1 (Tháng 1-3)' },
  { id: 2, name: 'Quý 2 (Tháng 4-6)' },
  { id: 3, name: 'Quý 3 (Tháng 7-9)' },
  { id: 4, name: 'Quý 4 (Tháng 10-12)' }
]);

const years = computed(() => {
  const currentYear = new Date().getFullYear();
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

// Computed cho tiêu đề báo cáo
const reportTitle = computed(() => {
  if (filters.timeFrame === 'month') {
    return `${months.value.find(m => m.id === filters.month)?.name} ${filters.year}`;
  } else if (filters.timeFrame === 'quarter') {
    return `${quarters.value.find(q => q.id === filters.quarter)?.name}, ${filters.year}`;
  } else {
    return `Năm ${filters.year}`;
  }
});

// Cập nhật phương thức loadData để hỗ trợ nhiều timeframe
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Tùy thuộc vào timeFrame, gọi API phù hợp
    if (filters.timeFrame === 'month') {
      // Báo cáo tháng
      reportData.value = await financialReports.getMonthlyReport(filters.year, filters.month);
    } else if (filters.timeFrame === 'quarter') {
      // Báo cáo quý
      reportData.value = await financialReports.getQuarterlyReport(filters.year, filters.quarter);
    } else {
      // Báo cáo năm
      reportData.value = await financialReports.getYearlyReport(filters.year);
    }
  } catch (err) {
    console.error('Error loading report data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Tính toán các giá trị tổng cộng cho bảng dữ liệu
const totals = computed(() => {
  if (!reportData.value) return null;
  
  // Nếu là báo cáo tháng (đối tượng đơn)
  if (!Array.isArray(reportData.value)) {
    return {
      totalBudget: filters.reportType === 'income' 
        ? reportData.value.summary.totalIncomeBudget 
        : reportData.value.summary.totalExpenseBudget,
      totalActual: filters.reportType === 'income'
        ? reportData.value.summary.totalIncomeActual
        : reportData.value.summary.totalExpenseActual
    };
  }
  
  // Nếu là báo cáo quý hoặc năm (mảng)
  return {
    totalBudget: reportData.value.reduce((sum, month) => 
      sum + (filters.reportType === 'income' 
        ? month.summary.totalIncomeBudget 
        : month.summary.totalExpenseBudget), 0),
    totalActual: reportData.value.reduce((sum, month) => 
      sum + (filters.reportType === 'income' 
        ? month.summary.totalIncomeActual 
        : month.summary.totalExpenseActual), 0)
  };
});

// Lấy danh sách danh mục
const categories = computed(() => {
  if (!reportData.value) return [];
  
  // Nếu là báo cáo tháng (đối tượng đơn)
  if (!Array.isArray(reportData.value)) {
    return filters.reportType === 'income' 
      ? reportData.value.incomeCategories 
      : reportData.value.expenseCategories;
  }
  
  // Nếu là báo cáo quý hoặc năm, ghép danh mục từ các tháng
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
  const totalActual = totals.value.totalActual;
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

const applyFilter = () => {
  loadData();
};

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

const getKpiClass = (value) => {
  if (value > 0) return 'positive';
  if (value < 0) return 'negative';
  return '';
};

const getKpiIcon = (value) => {
  if (value > 0) return 'bi-arrow-up-right';
  if (value < 0) return 'bi-arrow-down-right';
  return 'bi-dash';
};

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
    <template #page-title>Báo cáo Chi tiết Theo Danh mục</template>
    
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
              <label class="form-label">Khoảng thời gian</label>
              <select v-model="filters.timeFrame" class="form-select">
                <option value="month">Theo tháng</option>
                <option value="quarter">Theo quý</option>
                <option value="year">Theo năm</option>
              </select>
            </div>
            
            <!-- Hiển thị bộ lọc tháng nếu xem theo tháng -->
            <div v-if="filters.timeFrame === 'month'" class="form-group">
              <label class="form-label">Tháng</label>
              <select v-model="filters.month" class="form-select">
                <option v-for="month in months" :key="month.id" :value="month.id">{{ month.name }}</option>
              </select>
            </div>
            
            <!-- Hiển thị bộ lọc quý nếu xem theo quý -->
            <div v-if="filters.timeFrame === 'quarter'" class="form-group">
              <label class="form-label">Quý</label>
              <select v-model="filters.quarter" class="form-select">
                <option v-for="quarter in quarters" :key="quarter.id" :value="quarter.id">{{ quarter.name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Loại báo cáo</label>
              <select v-model="filters.reportType" class="form-select">
                <option value="income">Thu nhập</option>
                <option value="expense">Chi tiêu</option>
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
        <!-- Report header -->
        <div class="report-header">
          <h2 class="report-title">
            {{ filters.reportType === 'income' ? 'Báo cáo thu nhập' : 'Báo cáo chi tiêu' }} theo danh mục
          </h2>
          <h3 class="report-subtitle">
            {{ reportTitle }}
          </h3>
        </div>
        
        <!-- KPI Cards -->
        <div class="kpi-cards">
          <!-- Total card -->
          <div class="kpi-card">
            <div class="kpi-title">Tổng {{ filters.reportType === 'income' ? 'thu nhập' : 'chi tiêu' }}</div>
            <div class="kpi-value">{{ formatCurrency(totals.totalActual) }}</div>
            <div 
              class="kpi-comparison" 
              :class="getKpiClass(totals.totalActual - totals.totalBudget)"
            >
              <i :class="['bi', getKpiIcon(totals.totalActual - totals.totalBudget)]"></i>
              {{ formatCurrency(Math.abs(totals.totalActual - totals.totalBudget)) }}
              so với kế hoạch
            </div>
            <div class="kpi-detail">
              {{ totals.totalBudget > 0 ? ((totals.totalActual / totals.totalBudget) * 100).toFixed(1) : 0 }}% 
              kế hoạch đạt được
            </div>
          </div>
          
          <!-- Categories count -->
          <div class="kpi-card">
            <div class="kpi-title">Số danh mục</div>
            <div class="kpi-value">{{ categories.length }}</div>
            <div class="kpi-detail">
              Có {{ categories.length }} danh mục {{ filters.reportType === 'income' ? 'thu nhập' : 'chi tiêu' }}
            </div>
          </div>
          
          <!-- Top category -->
          <div class="kpi-card" v-if="categories.length > 0">
            <div class="kpi-title">Danh mục lớn nhất</div>
            <div class="kpi-value">{{ categories[0].categoryName }}</div>
            <div class="kpi-detail">
              {{ formatCurrency(categories[0].actualAmount) }}
              ({{ categories[0].percentageOfTotal.toFixed(1) }}% tổng giá trị)
            </div>
          </div>
        </div>
        
        <!-- Monthly Data Table when viewing quarterly or yearly -->
        <div v-if="filters.timeFrame !== 'month' && Array.isArray(reportData)" class="table-section">
          <h3 class="section-title">Chi tiết theo tháng</h3>
          <div class="table-container">
            <table class="monthly-table">
              <thead>
                <tr>
                  <th>Tháng</th>
                  <th class="text-right">Kế hoạch</th>
                  <th class="text-right">Thực tế</th>
                  <th class="text-right">Chênh lệch</th>
                  <th class="text-right">% Đạt được</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(month, index) in reportData" :key="index">
                  <td class="month-name">Tháng {{ month.month }}</td>
                  <td class="text-right">
                    {{ formatCurrency(filters.reportType === 'income' 
                      ? month.summary.totalIncomeBudget 
                      : month.summary.totalExpenseBudget) }}
                  </td>
                  <td class="text-right">
                    {{ formatCurrency(filters.reportType === 'income' 
                      ? month.summary.totalIncomeActual 
                      : month.summary.totalExpenseActual) }}
                  </td>
                  <td 
                    class="text-right" 
                    :class="getKpiClass(
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeActual 
                        : month.summary.totalExpenseActual) - 
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeBudget 
                        : month.summary.totalExpenseBudget)
                    )"
                  >
                    <i :class="['bi', getKpiIcon(
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeActual 
                        : month.summary.totalExpenseActual) - 
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeBudget 
                        : month.summary.totalExpenseBudget)
                    )]"></i>
                    {{ formatCurrency(Math.abs(
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeActual 
                        : month.summary.totalExpenseActual) - 
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeBudget 
                        : month.summary.totalExpenseBudget)
                    )) }}
                  </td>
                  <td class="text-right">
                    {{ 
                      (filters.reportType === 'income' 
                        ? month.summary.totalIncomeBudget 
                        : month.summary.totalExpenseBudget) > 0
                      ? (((filters.reportType === 'income' 
                          ? month.summary.totalIncomeActual 
                          : month.summary.totalExpenseActual) / 
                        (filters.reportType === 'income' 
                          ? month.summary.totalIncomeBudget 
                          : month.summary.totalExpenseBudget)) * 100).toFixed(1)
                      : 0 
                    }}%
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>Tổng cộng</th>
                  <th class="text-right">{{ formatCurrency(totals.totalBudget) }}</th>
                  <th class="text-right">{{ formatCurrency(totals.totalActual) }}</th>
                  <th 
                    class="text-right"
                    :class="getKpiClass(totals.totalActual - totals.totalBudget)"
                  >
                    <i :class="['bi', getKpiIcon(totals.totalActual - totals.totalBudget)]"></i>
                    {{ formatCurrency(Math.abs(totals.totalActual - totals.totalBudget)) }}
                  </th>
                  <th class="text-right">
                    {{ totals.totalBudget > 0 ? ((totals.totalActual / totals.totalBudget) * 100).toFixed(1) : 0 }}%
                  </th>
                </tr>
              </tfoot>
            </table>
          </div>
          
          <!-- Sau bảng tổng kết theo tháng, thêm nút toggle -->
          <div class="toggle-month-details" v-if="filters.timeFrame !== 'month' && Array.isArray(reportData)">
            <button @click="toggleMonthlyDetails" class="toggle-details-btn">
              <i :class="['bi', showMonthlyDetails ? 'bi-chevron-up' : 'bi-chevron-down']"></i>
              {{ showMonthlyDetails ? 'Ẩn chi tiết từng tháng' : 'Hiển thị chi tiết từng tháng' }}
            </button>
          </div>
          
          <!-- Cập nhật phần hiển thị chi tiết từng tháng -->
          <div v-if="filters.timeFrame !== 'month' && Array.isArray(reportData) && showMonthlyDetails" class="monthly-details">
            <!-- Chi tiết từng tháng như đã thêm trước đó -->
            <div v-for="(month, monthIndex) in reportData" :key="'month-'+monthIndex" class="month-detail-section">
              <h4 @click="toggleMonthDetail(month.month)" class="month-title clickable">
                {{ months.find(m => m.id === month.month)?.name }} {{ month.year }}
                <span class="month-total">
                  Tổng: {{ formatCurrency(filters.reportType === 'income' 
                    ? month.summary.totalIncomeActual 
                    : month.summary.totalExpenseActual) }}
                </span>
                <i :class="['bi', activeMonths.has(month.month) ? 'bi-chevron-up' : 'bi-chevron-down']"></i>
              </h4>
              
              <div v-if="activeMonths.has(month.month)" class="table-container">
                <!-- Bảng chi tiết danh mục của từng tháng -->
                <table class="categories-table month-categories-table">
                  <thead>
                    <tr>
                      <th>Danh mục</th>
                      <th class="text-right">Kế hoạch</th>
                      <th class="text-right">Thực tế</th>
                      <th class="text-right">Chênh lệch</th>
                      <th class="text-right">% Tổng</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="category in (filters.reportType === 'income' 
                      ? month.incomeCategories 
                      : month.expenseCategories)" 
                      :key="'month-'+month.month+'-cat-'+category.categoryId">
                      <td class="category-name">{{ category.categoryName }}</td>
                      <td class="text-right">{{ formatCurrency(category.budgetAmount) }}</td>
                      <td class="text-right">{{ formatCurrency(category.actualAmount) }}</td>
                      <td 
                        class="text-right" 
                        :class="getKpiClass(category.actualAmount - category.budgetAmount)"
                      >
                        <i :class="['bi', getKpiIcon(category.actualAmount - category.budgetAmount)]"></i>
                        {{ formatCurrency(Math.abs(category.actualAmount - category.budgetAmount)) }}
                      </td>
                      <td class="text-right">{{ category.percentageOfTotal.toFixed(1) }}%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Categories Table -->
        <div class="table-section">
          <h3 class="section-title">Chi tiết theo danh mục</h3>
          <div class="table-container">
            <table class="categories-table">
              <thead>
                <tr>
                  <th>Danh mục</th>
                  <th class="text-right">Kế hoạch</th>
                  <th class="text-right">Thực tế</th>
                  <th class="text-right">Chênh lệch</th>
                  <th class="text-right">% Tổng</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="category in categories" :key="category.categoryId">
                  <td class="category-name">{{ category.categoryName }}</td>
                  <td class="text-right">{{ formatCurrency(category.budgetAmount) }}</td>
                  <td class="text-right">{{ formatCurrency(category.actualAmount) }}</td>
                  <td 
                    class="text-right" 
                    :class="getKpiClass(category.actualAmount - category.budgetAmount)"
                  >
                    <i :class="['bi', getKpiIcon(category.actualAmount - category.budgetAmount)]"></i>
                    {{ formatCurrency(Math.abs(category.actualAmount - category.budgetAmount)) }}
                  </td>
                  <td class="text-right">{{ category.percentageOfTotal.toFixed(1) }}%</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>Tổng cộng</th>
                  <th class="text-right">{{ formatCurrency(totals.totalBudget) }}</th>
                  <th class="text-right">{{ formatCurrency(totals.totalActual) }}</th>
                  <th 
                    class="text-right"
                    :class="getKpiClass(totals.totalActual - totals.totalBudget)"
                  >
                    <i :class="['bi', getKpiIcon(totals.totalActual - totals.totalBudget)]"></i>
                    {{ formatCurrency(Math.abs(totals.totalActual - totals.totalBudget)) }}
                  </th>
                  <th class="text-right">100%</th>
                </tr>
              </tfoot>
            </table>
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

.view-toggle {
  display: flex;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.toggle-btn {
  flex: 1;
  padding: 0.5rem;
  border: none;
  background-color: #f9fafb;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.toggle-btn.active {
  background-color: #3b82f6;
  color: white;
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

.report-header {
  margin-bottom: 1.5rem;
  text-align: center;
}

.report-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
  color: #1f2937;
}

.report-subtitle {
  font-size: 1.25rem;
  font-weight: 500;
  color: #4b5563;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.kpi-card {
  padding: 1.5rem;
  background-color: #f9fafb;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.kpi-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.kpi-comparison {
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.25rem;
}

.kpi-comparison.positive {
  color: #10b981;
}

.kpi-comparison.negative {
  color: #ef4444;
}

.kpi-detail {
  font-size: 0.875rem;
  color: #6b7280;
}

.visualization-section, 
.table-section {
  margin-top: 2rem;
}

.chart-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  padding: 1rem;
}

.chart-header {
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.chart-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
}

.chart-body {
  padding: 1rem;
  height: 400px;
}

.table-container {
  overflow-x: auto;
}

.categories-table {
  width: 100%;
  border-collapse: collapse;
}

.categories-table th, 
.categories-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.categories-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #374151;
  text-align: left;
}

.categories-table tfoot th {
  background-color: #f3f4f6;
  font-weight: 700;
}

.text-right {
  text-align: right;
}

.category-name {
  font-weight: 500;
}

.positive {
  color: #10b981;
}

.negative {
  color: #ef4444;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
  
  .kpi-cards {
    grid-template-columns: 1fr;
  }
}

/* Add new styles for monthly-table */
.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 1rem;
}

.monthly-table {
  width: 100%;
  border-collapse: collapse;
}

.monthly-table th, 
.monthly-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.monthly-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #374151;
  text-align: left;
}

.monthly-table tfoot th {
  background-color: #f3f4f6;
  font-weight: 700;
}

.month-name {
  font-weight: 500;
}

/* Add new styles for month-detail-section */
.month-detail-section {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px dashed #e5e7eb;
}

.month-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.month-total {
  font-size: 0.9rem;
  color: #6b7280;
}

.month-categories-table {
  margin-bottom: 0;
}

.month-categories-table th {
  font-size: 0.9rem;
}

.month-categories-table td {
  font-size: 0.9rem;
}

/* Thêm nút hiển thị/ẩn chi tiết từng tháng */
.toggle-month-details {
  margin-top: 1rem;
  margin-bottom: 1rem;
  text-align: center;
}

.toggle-details-btn {
  padding: 0.5rem 1rem;
  background-color: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  margin: 0 auto;
}

.toggle-details-btn i {
  margin-right: 0.5rem;
}

.toggle-details-btn:hover {
  background-color: #e5e7eb;
}

/* Thêm styles mới */
.clickable {
  cursor: pointer;
}

.month-title:hover {
  background-color: #f9fafb;
}

/* Màu xen kẽ cho các tháng */
.month-detail-section:nth-child(odd) {
  background-color: #f9fafb;
}

/* Thêm màu sắc cho các tháng để phân biệt */
.month-title {
  border-left: 4px solid;
  padding-left: 0.5rem;
}

.month-detail-section:nth-child(1) .month-title,
.month-detail-section:nth-child(5) .month-title,
.month-detail-section:nth-child(9) .month-title {
  border-color: #3b82f6; /* blue */
}

.month-detail-section:nth-child(2) .month-title,
.month-detail-section:nth-child(6) .month-title,
.month-detail-section:nth-child(10) .month-title {
  border-color: #10b981; /* green */
}

.month-detail-section:nth-child(3) .month-title,
.month-detail-section:nth-child(7) .month-title,
.month-detail-section:nth-child(11) .month-title {
  border-color: #f59e0b; /* amber */
}

.month-detail-section:nth-child(4) .month-title,
.month-detail-section:nth-child(8) .month-title,
.month-detail-section:nth-child(12) .month-title {
  border-color: #ef4444; /* red */
}
</style>
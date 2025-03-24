<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { customerReports } from '../../api/customerReports';
import { supplierReports } from '../../api/supplierReports';

// Filters state - simplified to only report type
const filters = reactive({
  year: new Date().getFullYear(), // Always use current year
  reportType: 'customer' // 'customer' or 'supplier'
});

// Data state
const reportData = ref(null);
const isLoading = ref(false);
const error = ref(null);

// UI State
const activeTab = ref('month'); // 'month' or 'quarter'

// Load report data
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    let responseData;
    
    if (filters.reportType === 'customer') {
      // Load customer reports for current year
      responseData = await customerReports.getYearlyReport(filters.year);
    } else {
      // Load supplier reports for current year
      responseData = await supplierReports.getYearlyReport(filters.year);
    }
    
    if (responseData) {
      reportData.value = responseData;
    }
  } catch (err) {
    console.error('Error loading report data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Helper function to safely get entity name
const entityName = (entity) => {
  if (filters.reportType === 'customer') {
    return entity.customerName || 'Khách hàng không xác định';
  } else {
    return entity.supplierName || 'Nhà cung cấp không xác định';
  }
};

// Calculate total amount correctly by summing entity totals
const calculateTotalAmount = () => {
  if (!reportData.value || !reportData.value.entities || !reportData.value.entities.length) {
    return 0;
  }
  
  return reportData.value.entities.reduce((total, entity) => {
    return total + (entity.totalAmount || 0);
  }, 0);
};

// Method to get the amount for a specific month from an entity
const getMonthlyAmount = (entity, month) => {
  if (!entity.transactionsByMonth) return formatCurrency(0);
  
  const monthData = entity.transactionsByMonth.find(
    t => t.month === month && t.year === filters.year
  );
  
  return formatCurrency(monthData ? monthData.amount : 0);
};

// Method to get the amount for a specific quarter from an entity
const getQuarterlyAmount = (entity, quarter) => {
  if (!entity.transactionsByMonth) return formatCurrency(0);
  
  // Calculate months in the quarter
  const startMonth = (quarter - 1) * 3 + 1;
  const endMonth = startMonth + 2;
  
  // Sum all transactions in those months
  let quarterlyTotal = 0;
  for (let month = startMonth; month <= endMonth; month++) {
    const monthData = entity.transactionsByMonth.find(
      t => t.month === month && t.year === filters.year
    );
    
    if (monthData) {
      quarterlyTotal += parseFloat(monthData.amount || 0);
    }
  }
  
  return formatCurrency(quarterlyTotal);
};

// Method to get the total amount for a specific month across all entities
const getMonthlyTotal = (month) => {
  if (!reportData.value || !reportData.value.entities) return formatCurrency(0);
  
  let total = 0;
  reportData.value.entities.forEach(entity => {
    if (entity.transactionsByMonth) {
      const monthData = entity.transactionsByMonth.find(
        t => t.month === month && t.year === filters.year
      );
      
      if (monthData) {
        total += parseFloat(monthData.amount || 0);
      }
    }
  });
  
  return formatCurrency(total);
};

// Method to get the total amount for a specific quarter across all entities
const getQuarterlyTotal = (quarter) => {
  if (!reportData.value || !reportData.value.entities) return formatCurrency(0);
  
  // Calculate months in the quarter
  const startMonth = (quarter - 1) * 3 + 1;
  const endMonth = startMonth + 2;
  
  let total = 0;
  reportData.value.entities.forEach(entity => {
    if (entity.transactionsByMonth) {
      for (let month = startMonth; month <= endMonth; month++) {
        const monthData = entity.transactionsByMonth.find(
          t => t.month === month && t.year === filters.year
        );
        
        if (monthData) {
          total += parseFloat(monthData.amount || 0);
        }
      }
    }
  });
  
  return formatCurrency(total);
};

// Format currency
const formatCurrency = (value) => {
  if (!value) return '0 đ';
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    maximumFractionDigits: 0 
  }).format(value);
};

// Format percentage
const formatPercentage = (value) => {
  if (!value) return '0%';
  return `${Math.round(value)}%`;
};

// Load initial data
onMounted(() => {
  // Ensure DOM is rendered before loading data
  nextTick(() => {
    loadData();
  });
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo KH/NCC</template>
    
    <div class="content-container">
      <h2>Báo cáo theo khách hàng/nhà cung cấp</h2>
      <p>Trang này hiển thị báo cáo thống kê giao dịch dựa trên khách hàng và nhà cung cấp.</p>
      
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
              <label class="form-label">Loại báo cáo</label>
              <select v-model="filters.reportType" class="form-select" @change="loadData">
                <option value="customer">8.6. Thu nhập theo khách hàng</option>
                <option value="supplier">8.7. Chi phí theo nhà cung cấp</option>
              </select>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Hiển thị báo cáo theo thiết kế mới -->
      <div class="report-container" v-if="!isLoading && !error && reportData">
        <!-- Tiêu đề báo cáo -->
        <h2 class="report-section-title">
          {{ filters.reportType === 'customer' ? '8.6. Thu nhập theo Khách hàng' : '8.7. Chi phí theo Nhà cung cấp' }}
        </h2>
        <p class="report-section-subtitle">Thông tin được sắp xếp theo tổng {{ filters.reportType === 'customer' ? 'thu nhập' : 'chi phí' }} năm {{ filters.year }}</p>
        
        <!-- Tab điều hướng -->
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
        
        <!-- Bảng dữ liệu cho xem theo tháng -->
        <div v-if="activeTab === 'month'" class="report-table-container">
          <table class="report-table">
            <thead>
              <tr>
                <th>{{ filters.reportType === 'customer' ? 'Khách hàng' : 'Nhà cung cấp' }}</th>
                <th v-for="month in 12" :key="month">T{{ month }}</th>
                <th>Tổng năm</th>
                <th>%</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entity in reportData.entities" :key="entity.id || (filters.reportType === 'customer' ? entity.customerId : entity.supplierId)">
                <td>{{ entityName(entity) }}</td>
                <!-- Hiển thị số tiền theo từng tháng -->
                <template v-for="month in 12" :key="month">
                  <td>
                    {{ getMonthlyAmount(entity, month) }}
                  </td>
                </template>
                <td>{{ formatCurrency(entity.totalAmount) }}</td>
                <td>{{ formatPercentage(entity.percentage) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <th>Tổng cộng</th>
                <!-- Hiển thị tổng số tiền theo từng tháng -->
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
        
        <!-- Bảng dữ liệu cho xem theo quý -->
        <div v-if="activeTab === 'quarter'" class="report-table-container">
          <table class="report-table">
            <thead>
              <tr>
                <th>{{ filters.reportType === 'customer' ? 'Khách hàng' : 'Nhà cung cấp' }}</th>
                <th>Q1</th>
                <th>Q2</th>
                <th>Q3</th>
                <th>Q4</th>
                <th>Tổng năm</th>
                <th>%</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entity in reportData.entities" :key="entity.id || (filters.reportType === 'customer' ? entity.customerId : entity.supplierId)">
                <td>{{ entityName(entity) }}</td>
                <!-- Hiển thị số tiền theo từng quý -->
                <template v-for="quarter in 4" :key="quarter">
                  <td>
                    {{ getQuarterlyAmount(entity, quarter) }}
                  </td>
                </template>
                <td>{{ formatCurrency(entity.totalAmount) }}</td>
                <td>{{ formatPercentage(entity.percentage) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <th>Tổng cộng</th>
                <!-- Hiển thị tổng số tiền theo từng quý -->
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
      
      <!-- Loading spinner -->
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

.report-section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.report-section-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1.5rem;
}

/* Tab navigation */
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
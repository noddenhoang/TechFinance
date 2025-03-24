<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { customerReports } from '../../api/customerReports';
import { supplierReports } from '../../api/supplierReports';

// Filters state
const filters = reactive({
  year: new Date().getFullYear(),
  timeFrame: 'month', // 'month', 'quarter'
  month: new Date().getMonth() + 1,
  quarter: Math.floor(new Date().getMonth() / 3) + 1,
  reportType: 'customer' // 'customer' hoặc 'supplier'
});

// Data state
const reportData = ref(null);
const isLoading = ref(false);
const error = ref(null);

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

// Computed for report title
const reportTitle = computed(() => {
  const timeFrameText = filters.timeFrame === 'month'
    ? `${months.value.find(m => m.id === filters.month)?.name} ${filters.year}`
    : `${quarters.value.find(q => q.id === filters.quarter)?.name}, ${filters.year}`;
    
  const reportTypeText = filters.reportType === 'customer'
    ? 'Khách hàng'
    : 'Nhà cung cấp';
    
  return `Báo cáo ${reportTypeText} - ${timeFrameText}`;
});

// Load report data
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    let responseData;
    
    if (filters.reportType === 'customer') {
      // Load customer reports
      if (filters.timeFrame === 'month') {
        responseData = await customerReports.getMonthlyReport(filters.year, filters.month);
      } else {
        responseData = await customerReports.getQuarterlyReport(filters.year, filters.quarter);
      }
    } else {
      // Load supplier reports
      if (filters.timeFrame === 'month') {
        responseData = await supplierReports.getMonthlyReport(filters.year, filters.month);
      } else {
        responseData = await supplierReports.getQuarterlyReport(filters.year, filters.quarter);
      }
    }
    
    // Xử lý dữ liệu trả về để chỉ hiển thị tháng/quý đã chọn
    if (responseData) {
      // Chuẩn bị dữ liệu cho hiển thị
      reportData.value = processReportData(responseData);
    }
  } catch (err) {
    console.error('Error loading report data:', err);
    error.value = 'Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.';
  } finally {
    isLoading.value = false;
  }
};

// Thêm hàm xử lý dữ liệu để lọc theo thời gian đã chọn
const processReportData = (data) => {
  if (!data || !data.entities) return data;
  
  // Sao chép đối tượng dữ liệu để không ảnh hưởng đến dữ liệu gốc
  const processedData = { ...data };
  
  // Lọc transactionsByMonth cho từng entity
  processedData.entities = data.entities.map(entity => {
    const processedEntity = { ...entity };
    
    if (processedEntity.transactionsByMonth && processedEntity.transactionsByMonth.length > 0) {
      if (filters.timeFrame === 'month') {
        // Nếu xem theo tháng, chỉ hiển thị tháng đã chọn
        processedEntity.transactionsByMonth = processedEntity.transactionsByMonth.filter(
          trans => trans.year === filters.year && trans.month === filters.month
        );
      } else {
        // Nếu xem theo quý, chỉ hiển thị các tháng trong quý đã chọn
        const startMonth = (filters.quarter - 1) * 3 + 1;
        const endMonth = startMonth + 2;
        processedEntity.transactionsByMonth = processedEntity.transactionsByMonth.filter(
          trans => trans.year === filters.year && 
                 trans.month >= startMonth && 
                 trans.month <= endMonth
        );
      }
    }
    
    return processedEntity;
  });
  
  return processedData;
};

// Format currency
const formatCurrency = (value) => {
  if (!value) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

// Format percentage
const formatPercentage = (value) => {
  if (!value) return '0%';
  return `${value.toFixed(2)}%`;
};

// Apply filter
const applyFilter = () => {
  loadData();
};

// Toggle detail view for a specific entity
const expandedEntities = reactive(new Set());
const toggleEntityDetail = (entityId) => {
  if (expandedEntities.has(entityId)) {
    expandedEntities.delete(entityId);
  } else {
    expandedEntities.add(entityId);
  }
};

// Load initial data
onMounted(() => {
  // Tạm thời để loadData trống để tránh lỗi khi chưa có API
  // loadData();
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo KH/NCC</template>
    
    <div class="content-box">
      <h2>Báo cáo theo khách hàng/nhà cung cấp</h2>
      <p>Trang này hiển thị báo cáo thống kê giao dịch dựa trên khách hàng và nhà cung cấp.</p>
      
      <!-- Bộ lọc báo cáo -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="fas fa-filter"></i> Bộ lọc báo cáo
          </h3>
        </div>
        <div class="filter-content">
          <div class="filter-grid">
            <div class="form-group">
              <label class="form-label">Loại báo cáo</label>
              <select v-model="filters.reportType" class="form-select">
                <option value="customer">Thu nhập theo khách hàng</option>
                <option value="supplier">Chi phí theo nhà cung cấp</option>
              </select>
            </div>
            
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
          </div>
          
          <div class="filter-actions">
            <button @click="applyFilter" class="btn btn-primary">
              <i class="fas fa-search"></i> Áp dụng
            </button>
          </div>
        </div>
      </div>
      
      <!-- Hiển thị báo cáo -->
      <div class="report-container">
        <div v-if="isLoading" class="loading-indicator">
          <div class="spinner">
            <div class="bounce1"></div>
            <div class="bounce2"></div>
            <div class="bounce3"></div>
          </div>
          <p>Đang tải dữ liệu báo cáo...</p>
        </div>
        
        <div v-else-if="error" class="error-message">
          <i class="fas fa-exclamation-triangle"></i>
          <p>{{ error }}</p>
        </div>
        
        <div v-else-if="reportData" class="report-data">
          <h3 class="report-title">{{ reportTitle }}</h3>
          
          <!-- Thông tin tổng quan -->
          <div class="summary-cards">
            <div class="summary-card">
              <div class="card-label">Tổng {{ filters.reportType === 'customer' ? 'thu nhập' : 'chi phí' }}</div>
              <div class="card-value">{{ formatCurrency(reportData.totalAmount) }}</div>
            </div>
            
            <div class="summary-card">
              <div class="card-label">{{ filters.reportType === 'customer' ? 'Đã nhận' : 'Đã trả' }}</div>
              <div class="card-value">{{ formatCurrency(filters.reportType === 'customer' ? reportData.receivedAmount : reportData.paidAmount) }}</div>
            </div>
            
            <div class="summary-card">
              <div class="card-label">{{ filters.reportType === 'customer' ? 'Chưa nhận' : 'Chưa trả' }}</div>
              <div class="card-value">{{ formatCurrency(filters.reportType === 'customer' ? reportData.pendingAmount : reportData.unpaidAmount) }}</div>
            </div>
          </div>
          
          <!-- Bảng chi tiết -->
          <div v-if="reportData.entities && reportData.entities.length > 0" class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ filters.reportType === 'customer' ? 'Khách hàng' : 'Nhà cung cấp' }}</th>
                  <th>Tổng {{ filters.reportType === 'customer' ? 'thu nhập' : 'chi phí' }}</th>
                  <th>{{ filters.reportType === 'customer' ? 'Đã nhận' : 'Đã trả' }}</th>
                  <th>{{ filters.reportType === 'customer' ? 'Chưa nhận' : 'Chưa trả' }}</th>
                  <th>Tỷ lệ</th>
                  <th>Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="entity in reportData.entities" :key="entity.customerId || entity.supplierId">
                  <tr>
                    <td>{{ filters.reportType === 'customer' ? entity.customerName : entity.supplierName }}</td>
                    <td>{{ formatCurrency(entity.totalAmount) }}</td>
                    <td>{{ formatCurrency(filters.reportType === 'customer' ? entity.receivedAmount : entity.paidAmount) }}</td>
                    <td>{{ formatCurrency(filters.reportType === 'customer' ? entity.pendingAmount : entity.unpaidAmount) }}</td>
                    <td>{{ formatPercentage(entity.percentage) }}</td>
                    <td>
                      <button @click="toggleEntityDetail(filters.reportType === 'customer' ? entity.customerId : entity.supplierId)" 
                              class="btn btn-sm btn-outline-primary">
                        <i :class="expandedEntities.has(filters.reportType === 'customer' ? entity.customerId : entity.supplierId) ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                  
                  <!-- Chi tiết -->
                  <template v-if="expandedEntities.has(entity.customerId || entity.supplierId)">
                    <tr>
                      <td colspan="6" class="p-0">
                        <div class="entity-details">
                          <!-- Hiển thị chi tiết theo tháng -->
                          <div class="detail-section" v-if="entity.transactionsByMonth && entity.transactionsByMonth.length > 0">
                            <h5>Chi tiết theo tháng</h5>
                            <div class="table-responsive">
                              <table class="table table-sm">
                                <thead>
                                  <tr>
                                    <th>Thời gian</th>
                                    <th>Số giao dịch</th>
                                    <th>Tổng {{ filters.reportType === 'customer' ? 'thu nhập' : 'chi phí' }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr v-for="month in entity.transactionsByMonth" :key="`${month.year}-${month.month}`">
                                    <td>{{ filters.timeFrame === 'month' ? 'Tháng ' + month.month + '/' + month.year : 'Quý ' + Math.ceil(month.month/3) + '/' + month.year }}</td>
                                    <td>{{ month.transactionCount }}</td>
                                    <td>{{ formatCurrency(month.amount) }}</td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                          </div>
                          
                          <!-- Hiển thị nếu không có dữ liệu -->
                          <div v-else class="no-data-message">
                            <i class="fas fa-info-circle"></i>
                            <p>Không có dữ liệu giao dịch cho khoảng thời gian này.</p>
                          </div>
                          
                          <!-- Chi tiết theo danh mục không thay đổi -->
                          <div class="detail-section" v-if="entity.transactionsByCategory && entity.transactionsByCategory.length > 0">
                            <h5>Chi tiết theo danh mục</h5>
                            <div class="table-responsive">
                              <table class="table table-sm">
                                <thead>
                                  <tr>
                                    <th>Danh mục</th>
                                    <th>Tổng {{ filters.reportType === 'customer' ? 'thu nhập' : 'chi phí' }}</th>
                                    <th>Tỷ lệ</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr v-for="category in entity.transactionsByCategory" :key="category.categoryId">
                                    <td>{{ category.categoryName }}</td>
                                    <td>{{ formatCurrency(category.amount) }}</td>
                                    <td>{{ formatPercentage(category.percentage) }}</td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  </template>
                </template>
              </tbody>
            </table>
          </div>
          
          <div v-else class="no-data">
            <i class="fas fa-info-circle"></i>
            <p>Không có dữ liệu cho khoảng thời gian này.</p>
          </div>
        </div>
        
        <div v-else class="no-data">
          <i class="fas fa-info-circle"></i>
          <p>Vui lòng chọn các bộ lọc và nhấn Áp dụng để xem báo cáo.</p>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-box {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.content-box h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 1rem;
}

.content-box p {
  color: #6b7280;
  line-height: 1.5;
}

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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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

.filter-actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  font-weight: 500;
  line-height: 1.5;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  color: white;
  background-color: #2563eb;
  border: 1px solid #2563eb;
}

.btn-primary:hover {
  background-color: #1d4ed8;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
  border-radius: 0.2rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.btn-outline-primary {
  color: #2563eb;
  background-color: transparent;
  border: 1px solid #2563eb;
}

.btn-outline-primary:hover {
  background-color: #2563eb;
  color: white;
}

.report-container {
  margin-top: 2rem;
}

.report-title {
  margin-bottom: 1.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.5rem;
}

.card-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.card-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: #111827;
}

.table-responsive {
  overflow-x: auto;
  margin-bottom: 1.5rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th {
  background-color: #f3f4f6;
  padding: 0.75rem 1rem;
  font-weight: 600;
  color: #374151;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.table-sm th,
.table-sm td {
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
}

.entity-details {
  background-color: #f9fafb;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.detail-section {
  margin-bottom: 1.5rem;
}

.detail-section h5 {
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.no-data-message {
  text-align: center;
  padding: 2rem;
  color: #6b7280;
}

.no-data-message i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  color: #9ca3af;
}

.loading-indicator,
.error-message,
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 0;
  text-align: center;
}

.spinner {
  margin-bottom: 1rem;
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
  
  .summary-cards {
    grid-template-columns: 1fr;
  }
}

/* Spinner animation */
.spinner {
  margin: 0 auto;
  width: 70px;
  text-align: center;
}

.spinner > div {
  width: 14px;
  height: 14px;
  background-color: #3b82f6;
  border-radius: 100%;
  display: inline-block;
  animation: sk-bouncedelay 1.4s infinite ease-in-out both;
}

.spinner .bounce1 {
  animation-delay: -0.32s;
}

.spinner .bounce2 {
  animation-delay: -0.16s;
}

@keyframes sk-bouncedelay {
  0%, 80%, 100% { 
    transform: scale(0);
  } 
  40% { 
    transform: scale(1.0);
  }
}
</style> 
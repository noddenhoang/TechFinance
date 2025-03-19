<script setup>
import { ref, reactive, onMounted } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { customers } from '../../api/customers';

// Data state
const customersList = ref([]);
const selectedCustomer = ref(null);
const loading = ref(true);
const error = ref(null);
const showDetails = ref(false);

// Filter state
const filters = reactive({
  name: ''
});

// Pagination state
const pagination = reactive({
  currentPage: 0,
  totalPages: 0,
  totalItems: 0,
  pageSize: 8
});

// Sorting state
const sorting = reactive({
  field: 'name',
  direction: 'asc'
});

// Load customers on component mount
onMounted(async () => {
  await loadCustomers();
});

// Fetch customers with pagination and filtering
async function loadCustomers(newPage = 0) {
  loading.value = true;
  error.value = null;
  
  try {
    // Ensure newPage is a valid number
    const targetPage = parseInt(newPage);
    const page = !isNaN(targetPage) ? targetPage : 0;
    
    const result = await customers.getAll(
      filters, 
      page, 
      pagination.pageSize,
      sorting.field,
      sorting.direction
    );
    
    console.log('API pagination response:', result);
    
    // Update data and pagination info
    customersList.value = result.content || [];
    
    // Update pagination info from API response
    if (result.page) {
      pagination.currentPage = result.page.page;
      pagination.totalPages = result.page.totalPages;
      pagination.totalItems = result.page.totalElement;
    } else {
      pagination.currentPage = page;
      pagination.totalPages = Math.ceil((result.totalElement || 0) / pagination.pageSize);
      pagination.totalItems = result.totalElement || 0;
    }
    
    // Close details view when changing pages
    closeDetails();
    
  } catch (err) {
    error.value = 'Không thể tải danh sách khách hàng. Vui lòng thử lại sau.';
    console.error('Error loading customers:', err);
  } finally {
    loading.value = false;
  }
}

// Navigation between pages
function goToPage(page) {
  // Ensure page is a valid number
  const targetPage = parseInt(page);
  
  // Check if it's a valid number and within allowed range
  if (!isNaN(targetPage) && targetPage >= 0 && targetPage < pagination.totalPages) {
    loadCustomers(targetPage);
  }
}

// Reset filters
function resetFilters() {
  filters.name = '';
  pagination.currentPage = 0; // Reset to first page
  loadCustomers(0);
  closeDetails();
}

// View customer details
async function showCustomerDetails(customer) {
  try {
    loading.value = true;
    // If we need to fetch more detailed information
    const detailedCustomer = await customers.getById(customer.id);
    selectedCustomer.value = detailedCustomer;
    showDetails.value = true;
  } catch (err) {
    console.error('Error fetching customer details:', err);
    error.value = 'Không thể tải thông tin chi tiết khách hàng.';
  } finally {
    loading.value = false;
  }
}

// Close details view
function closeDetails() {
  showDetails.value = false;
  selectedCustomer.value = null;
}
</script>

<template>
  <AppLayout>
    <template #page-title>Quản lý khách hàng</template>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý khách hàng</h2>
          <p class="page-description">Xem và tìm kiếm thông tin khách hàng</p>
        </div>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container" :class="{ 'minimized': showDetails }">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel"></i>
            Tìm kiếm khách hàng
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Tên khách hàng</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-search input-icon-left"></i>
                <input
                  v-model="filters.name"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập tên khách hàng để tìm kiếm"
                />
              </div>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilters" class="btn-outline">
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
            </button>
            <button @click.prevent="loadCustomers(0)" class="btn-primary">
              <i class="bi bi-search"></i>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
      
      <!-- Customer List and Details Container -->
      <div class="content-layout" :class="{ 'with-details': showDetails }">
        <!-- Customers List -->
        <div class="card customers-list" :class="{ 'minimized': showDetails }">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-people"></i>
              Danh sách khách hàng
            </h3>
            <span class="result-count">{{ customersList.length }} khách hàng</span>
          </div>
          
          <div v-if="loading && !showDetails" class="card-empty-state">
            <div class="loading-spinner"></div>
            <p>Đang tải dữ liệu...</p>
          </div>
          
          <div v-else-if="error && !showDetails" class="card-empty-state error">
            <i class="bi bi-exclamation-circle error-icon"></i>
            <p>{{ error }}</p>
            <button @click="loadCustomers" class="btn-primary">
              <i class="bi bi-arrow-repeat"></i>
              Thử lại
            </button>
          </div>
          
          <div v-else-if="customersList.length === 0 && !showDetails" class="card-empty-state">
            <i class="bi bi-person-slash empty-icon"></i>
            <p>Không tìm thấy khách hàng nào</p>
            <p class="empty-description">Hãy điều chỉnh bộ lọc tìm kiếm</p>
          </div>
          
          <div v-else class="table-responsive">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Tên khách hàng</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="customer in customersList" 
                  :key="customer.id" 
                  @click="showCustomerDetails(customer)"
                  class="customer-row"
                  :class="{ 'active': selectedCustomer && selectedCustomer.id === customer.id }"
                >
                  <td class="customer-name">
                    <i class="bi bi-person customer-icon"></i>
                    {{ customer.name }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- Pagination Controls -->
          <div v-if="customersList.length > 0" class="pagination-container">
            <div class="pagination-info">
              Hiển thị {{ customersList.length }} trên tổng số {{ pagination.totalItems }} khách hàng
            </div>
            
            <div class="pagination-controls">
              <button 
                @click.prevent="goToPage(0)" 
                :disabled="pagination.currentPage === 0"
                class="pagination-btn"
                title="Trang đầu"
              >
                <i class="bi bi-chevron-double-left"></i>
              </button>
              
              <button 
                @click.prevent="goToPage(pagination.currentPage - 1)" 
                :disabled="pagination.currentPage === 0"
                class="pagination-btn"
                title="Trang trước"
              >
                <i class="bi bi-chevron-left"></i>
              </button>
              
              <span class="pagination-text">
                Trang {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
              </span>
              
              <button 
                @click.prevent="goToPage(pagination.currentPage + 1)" 
                :disabled="pagination.currentPage === pagination.totalPages - 1"
                class="pagination-btn"
                title="Trang sau"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
              
              <button 
                @click.prevent="goToPage(pagination.totalPages - 1)" 
                :disabled="pagination.currentPage === pagination.totalPages - 1"
                class="pagination-btn"
                title="Trang cuối"
              >
                <i class="bi bi-chevron-double-right"></i>
              </button>
            </div>
          </div>
        </div>
        
        <!-- Customer Details -->
        <div v-if="showDetails" class="card customer-details">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-person-vcard"></i>
              Chi tiết khách hàng
            </h3>
            <button @click="closeDetails" class="btn-close" title="Đóng chi tiết">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
          
          <div v-if="loading" class="card-empty-state">
            <div class="loading-spinner"></div>
            <p>Đang tải thông tin chi tiết...</p>
          </div>
          
          <div v-else-if="selectedCustomer" class="customer-details-content">
            <div class="detail-header">
              <div class="customer-avatar">
                <i class="bi bi-person-circle"></i>
              </div>
              <h3 class="customer-full-name">{{ selectedCustomer.name }}</h3>
            </div>
            
            <div class="detail-section">
              <h4 class="detail-section-title">Thông tin liên hệ</h4>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-envelope"></i>
                  Email:
                </div>
                <div class="detail-value">{{ selectedCustomer.email || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-telephone"></i>
                  Số điện thoại:
                </div>
                <div class="detail-value">{{ selectedCustomer.phone || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-geo-alt"></i>
                  Địa chỉ:
                </div>
                <div class="detail-value">{{ selectedCustomer.address || 'Chưa cung cấp' }}</div>
              </div>
            </div>
            
            <div class="detail-section">
              <h4 class="detail-section-title">Thông tin khác</h4>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-calendar-check"></i>
                  Trạng thái:
                </div>
                <div class="detail-value">
                  <span :class="['status-badge', selectedCustomer.isActive ? 'active' : 'inactive']">
                    <i :class="[selectedCustomer.isActive ? 'bi bi-check-circle' : 'bi bi-x-circle']"></i>
                    {{ selectedCustomer.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-card-text"></i>
                  Ghi chú:
                </div>
                <div class="detail-value">{{ selectedCustomer.notes || 'Không có ghi chú' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
/* Thiết kế container chung */
.content-container {
  padding: 1rem;
  max-width: 1200px;
  margin: 0 auto;
}

/* HEADER STYLES */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background-color: #f8fafc;
  border-radius: 0.5rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.25rem;
}

.page-description {
  font-size: 0.875rem;
  color: #64748b;
}

/* CARD STYLES */
.card {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.card-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8fafc;
}

.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-title i {
  color: #4f46e5;
}

.result-count {
  font-size: 0.875rem;
  color: #64748b;
  background-color: #e0e7ff;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-weight: 500;
}

/* FILTER STYLES */
.filter-container {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  margin-bottom: 1.5rem;
  overflow: hidden;
}

.filter-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  background-color: #f8fafc;
}

.filter-content {
  padding: 1.25rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.25rem;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f1f5f9;
}

/* FORM STYLES */
.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: white;
  font-size: 0.875rem;
  color: #1f2937;
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.2);
}

.form-input.has-icon {
  padding-left: 2.5rem;
}

.input-icon-wrapper {
  position: relative;
}

.input-icon-left {
  position: absolute;
  top: 50%;
  left: 0.875rem;
  transform: translateY(-50%);
  color: #6366f1;
  pointer-events: none;
  font-size: 1rem;
}

/* TABLE STYLES */
.table-responsive {
  overflow-x: auto;
}

.data-table {
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.data-table th,
.data-table td {
  padding: 1rem 1.25rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #4b5563;
  background-color: #f9fafb;
  letter-spacing: 0.05em;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.customer-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.customer-row:hover {
  background-color: #f1f5f9;
}

.customer-row.active {
  background-color: #eff6ff;
}

.customer-name {
  font-weight: 500;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.customer-icon {
  color: #4f46e5;
  font-size: 1rem;
}

/* CARD EMPTY STATE */
.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
  text-align: center;
}

.loading-spinner {
  width: 2.5rem;
  height: 2.5rem;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spinner 0.8s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spinner {
  to { transform: rotate(360deg); }
}

.empty-icon, .error-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.empty-icon {
  color: #94a3b8;
}

.error-icon {
  color: #ef4444;
}

.card-empty-state p {
  font-size: 1rem;
  font-weight: 500;
  color: #4b5563;
  margin: 0;
}

.empty-description {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.5rem !important;
}

.card-empty-state button {
  margin-top: 1.5rem;
}

.card-empty-state.error p {
  color: #b91c1c;
}

/* BUTTON STYLES */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: #4f46e5;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-primary:hover {
  background-color: #4338ca;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: white;
  color: #4f46e5;
  border: 1px solid #4f46e5;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-outline:hover {
  background-color: #f3f4f6;
}

.btn-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  background-color: transparent;
  color: #4b5563;
  border: none;
  border-radius: 0.375rem;
  font-size: 1.25rem;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-close:hover {
  background-color: #f3f4f6;
}

/* PAGINATION STYLES */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-top: 1px solid #e5e7eb;
  background-color: #f9fafb;
}

.pagination-info {
  color: #6b7280;
  font-size: 0.875rem;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: white;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  background-color: #f3f4f6;
  color: #4f46e5;
  border-color: #4f46e5;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-text {
  font-size: 0.875rem;
  color: #4b5563;
  padding: 0 0.5rem;
}

/* STATUS BADGES */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.status-badge.active {
  background-color: #dcfce7;
  color: #15803d;
}

.status-badge.inactive {
  background-color: #fee2e2;
  color: #b91c1c;
}

/* CUSTOMER DETAILS STYLES */
.content-layout {
  display: flex;
  flex-direction: column;
}

.content-layout.with-details {
  flex-direction: row;
  gap: 1.5rem;
}

.customers-list {
  flex: 1;
}

.customer-details {
  width: 100%;
  max-width: 30rem;
}

.customer-details-content {
  padding: 1.5rem;
}

.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1.5rem;
}

.customer-avatar {
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  background-color: #e0e7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.75rem;
}

.customer-avatar i {
  font-size: 2.5rem;
  color: #4f46e5;
}

.customer-full-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.detail-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 1.25rem;
  margin-bottom: 1.5rem;
}

.detail-section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-top: 0;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  margin-bottom: 1rem;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-label {
  min-width: 8rem;
  font-weight: 500;
  color: #4b5563;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.detail-label i {
  color: #6366f1;
  margin-top: 0.2rem;
}

.detail-value {
  flex: 1;
  color: #1e293b;
}

/* Responsive styles */
@media (max-width: 768px) {
  .content-layout.with-details {
    flex-direction: column;
  }
  
  .customers-list.minimized {
    display: none;
  }
  
  .customer-details {
    max-width: 100%;
  }
  
  .filter-container.minimized {
    display: none;
  }
}

@media (min-width: 768px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
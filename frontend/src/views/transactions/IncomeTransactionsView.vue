<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { incomeTransactions } from '../../api/incomeTransactions';
import { customers } from '../../api/customers';
import { incomeCategories } from '../../api/incomeCategories';
import { useAuthStore } from '../../stores/auth';
import { formatCurrency } from '../../utils/formatters';

// Data state
const transactionsList = ref([]);
const selectedTransaction = ref(null);
const loading = ref(true);
const error = ref(null);
const showDetails = ref(false);

// Data for dropdowns
const customersList = ref([]);
const categoriesList = ref([]);
const loadingCustomers = ref(false);
const loadingCategories = ref(false);

// Auth store để kiểm tra quyền admin
const auth = useAuthStore();

// Kiểm tra quyền admin
const isAdmin = computed(() => {
  // Debug dữ liệu auth nếu cần
  console.log('Auth user data:', auth.user);
  console.log('Auth authorities:', auth.user?.authorities);
  
  // Kiểm tra theo nhiều cấu trúc có thể có
  if (auth.user?.authorities && Array.isArray(auth.user.authorities)) {
    // Trường hợp 1: authorities là mảng string
    if (typeof auth.user.authorities[0] === 'string') {
      return auth.user.authorities.includes('ROLE_ADMIN');
    }
    
    // Trường hợp 2: authorities là mảng object có thuộc tính authority
    if (typeof auth.user.authorities[0] === 'object') {
      return auth.user.authorities.some(auth => 
        auth.authority === 'ROLE_ADMIN' || auth === 'ROLE_ADMIN'
      );
    }
  }
  
  // Trường hợp 3: role trực tiếp trên user
  if (auth.user?.role === 'ROLE_ADMIN' || auth.user?.role === 'admin') {
    return true;
  }
  
  // Không tìm thấy quyền admin
  return false;
});

// States for Add/Edit modal
const showModal = ref(false);
const editMode = ref(false);
const saving = ref(false);
const modalTransaction = reactive({
  id: null,
  categoryId: null,
  customerId: null,
  transactionDate: new Date().toISOString().slice(0, 10), // Today in YYYY-MM-DD format
  amount: '',
  paymentStatus: 'PENDING',
  description: '',
  referenceNo: ''
});
const modalErrors = reactive({
  categoryId: '',
  amount: '',
  transactionDate: '',
  referenceNo: ''
});

// States for Delete modal
const showDeleteModal = ref(false);
const transactionToDelete = ref(null);
const deleting = ref(false);

// Filter state
const filters = reactive({
  customerId: null,
  categoryId: null,
  startDate: null,
  endDate: null,
  minAmount: '',
  maxAmount: '',
  paymentStatus: '',
  referenceNo: ''
});

// Pagination state
const pagination = reactive({
  currentPage: 0,
  totalPages: 0,
  totalItems: 0,
  pageSize: 10
});

// Sorting state
const sorting = reactive({
  field: 'transactionDate',
  direction: 'desc'
});

// Format today's date for filter defaults
const today = new Date();
const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
filters.startDate = firstDayOfMonth.toISOString().slice(0, 10);
filters.endDate = today.toISOString().slice(0, 10);

// Payment status options
const paymentStatusOptions = [
  { value: '', label: 'Tất cả' },
  { value: 'RECEIVED', label: 'Đã nhận' },
  { value: 'PENDING', label: 'Chờ thanh toán' }
];

// Load transactions on component mount
onMounted(async () => {
  await Promise.all([
    loadTransactions(),
    loadCustomersList(),
    loadCategoriesList()
  ]);
});

// Fetch transactions with pagination and filtering
async function loadTransactions(newPage = 0) {
  loading.value = true;
  error.value = null;
  
  try {
    // Ensure newPage is a valid number
    const targetPage = parseInt(newPage);
    const page = !isNaN(targetPage) ? targetPage : 0;
    
    // Convert string amounts to numbers for the API if they are not empty
    const apiFilters = {...filters};
    if (apiFilters.minAmount === '') apiFilters.minAmount = null;
    if (apiFilters.maxAmount === '') apiFilters.maxAmount = null;
    
    const result = await incomeTransactions.getAll(
      apiFilters, 
      page, 
      pagination.pageSize,
      sorting.field,
      sorting.direction
    );
    
    console.log('API pagination response:', result);
    
    // Update data and pagination info
    transactionsList.value = result.content || [];
    
    // Update pagination info
    pagination.currentPage = result.pageNumber;
    pagination.totalPages = result.totalPages;
    pagination.totalItems = result.totalElements;
    
    // Close details view when changing pages
    closeDetails();
    
  } catch (err) {
    error.value = 'Không thể tải danh sách giao dịch thu nhập. Vui lòng thử lại sau.';
    console.error('Error loading transactions:', err);
  } finally {
    loading.value = false;
  }
}

// Load customers for dropdown
async function loadCustomersList() {
  loadingCustomers.value = true;
  try {
    // Use getAll with empty filters to get all active customers
    const response = await customers.getAll({isActive: true}, 0, 1000);
    customersList.value = response.content || [];
  } catch (err) {
    console.error('Error loading customers:', err);
  } finally {
    loadingCustomers.value = false;
  }
}

// Load categories for dropdown
async function loadCategoriesList() {
  loadingCategories.value = true;
  try {
    // Use getAll with empty filters to get all active categories
    const response = await incomeCategories.getAll({isActive: true}, 0, 1000);
    categoriesList.value = response.content || [];
  } catch (err) {
    console.error('Error loading income categories:', err);
  } finally {
    loadingCategories.value = false;
  }
}

// Navigation between pages
function goToPage(page) {
  // Ensure page is a valid number
  const targetPage = parseInt(page);
  
  // Check if it's a valid number and within allowed range
  if (!isNaN(targetPage) && targetPage >= 0 && targetPage < pagination.totalPages) {
    loadTransactions(targetPage);
  }
}

// Reset filters
function resetFilters() {
  const today = new Date();
  const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
  
  filters.customerId = null;
  filters.categoryId = null;
  filters.startDate = firstDayOfMonth.toISOString().slice(0, 10);
  filters.endDate = today.toISOString().slice(0, 10);
  filters.minAmount = '';
  filters.maxAmount = '';
  filters.paymentStatus = '';
  filters.referenceNo = '';
  
  pagination.currentPage = 0; // Reset to first page
  loadTransactions(0);
  closeDetails();
}

// Cập nhật function showTransactionDetails
async function showTransactionDetails(transaction) {
  try {
    loading.value = true;
    // Fetch detailed info if needed
    const detailedTransaction = await incomeTransactions.getById(transaction.id);
    selectedTransaction.value = detailedTransaction;
    showDetails.value = true; // Hiện modal thay vì thay đổi layout
  } catch (err) {
    console.error('Error fetching transaction details:', err);
    error.value = 'Không thể tải thông tin chi tiết giao dịch.';
  } finally {
    loading.value = false;
  }
}

// Đơn giản hóa closeDetails
function closeDetails() {
  showDetails.value = false;
  selectedTransaction.value = null;
}

// Format currency values
function formatAmountDisplay(amount) {
  return formatCurrency(amount);
}

// Show notification
const notification = reactive({
  show: false,
  message: '',
  type: 'success'
});

function showNotification(message, type = 'success') {
  notification.message = message;
  notification.type = type;
  notification.show = true;
  
  setTimeout(() => {
    notification.show = false;
  }, 3000); // Auto hide after 3 seconds
}

// Open modal to add a new transaction
function openAddModal() {
  editMode.value = false;
  
  // Reset form
  modalTransaction.id = null;
  modalTransaction.categoryId = null;
  modalTransaction.customerId = null;
  modalTransaction.transactionDate = new Date().toISOString().slice(0, 10);
  modalTransaction.amount = '';
  modalTransaction.paymentStatus = 'PENDING';
  modalTransaction.description = '';
  modalTransaction.referenceNo = '';
  
  // Clear errors
  Object.keys(modalErrors).forEach(key => modalErrors[key] = '');
  
  showModal.value = true;
  
  // Focus on first input after modal is visible
  nextTick(() => {
    document.querySelector('select[name="categoryId"]')?.focus();
  });
}

// Open modal to edit an existing transaction
function openEditModal(transaction) {
  editMode.value = true;
  
  // Fill form with transaction data
  modalTransaction.id = transaction.id;
  modalTransaction.categoryId = transaction.categoryId;
  modalTransaction.customerId = transaction.customerId;
  modalTransaction.transactionDate = transaction.transactionDate;
  modalTransaction.amount = transaction.amount;
  modalTransaction.paymentStatus = transaction.paymentStatus;
  modalTransaction.description = transaction.description || '';
  modalTransaction.referenceNo = transaction.referenceNo || '';
  
  // Clear errors
  Object.keys(modalErrors).forEach(key => modalErrors[key] = '');
  
  showModal.value = true;
  
  // Focus on first input after modal is visible
  nextTick(() => {
    document.querySelector('select[name="categoryId"]')?.focus();
  });
}

// Close add/edit modal
function closeModal() {
  showModal.value = false;
}

// Validate amount input to ensure it's numeric
function validateAmount() {
  modalTransaction.amount = modalTransaction.amount.replace(/[^0-9.]/g, '');
  
  if (modalTransaction.amount && !/^[0-9]+(\.[0-9]{0,2})?$/.test(modalTransaction.amount)) {
    const parts = modalTransaction.amount.split('.');
    if (parts.length > 1) {
      modalTransaction.amount = parts[0] + '.' + parts[1].substring(0, 2);
    }
  }
}

// Save transaction (create or update)
async function saveTransaction() {
  // Reset errors
  Object.keys(modalErrors).forEach(key => modalErrors[key] = '');
  
  // Validate
  let isValid = true;
  
  if (!modalTransaction.categoryId) {
    modalErrors.categoryId = 'Danh mục không được để trống';
    isValid = false;
  }
  
  if (!modalTransaction.transactionDate) {
    modalErrors.transactionDate = 'Ngày giao dịch không được để trống';
    isValid = false;
  }
  
  if (!modalTransaction.amount || parseFloat(modalTransaction.amount) <= 0) {
    modalErrors.amount = 'Số tiền phải lớn hơn 0';
    isValid = false;
  }
  
  if (!isValid) return;
  
  saving.value = true;
  try {
    const requestData = {
      categoryId: parseInt(modalTransaction.categoryId),
      customerId: modalTransaction.customerId ? parseInt(modalTransaction.customerId) : null,
      transactionDate: modalTransaction.transactionDate,
      amount: parseFloat(modalTransaction.amount),
      paymentStatus: modalTransaction.paymentStatus,
      description: modalTransaction.description,
      referenceNo: modalTransaction.referenceNo
    };
    
    if (editMode.value) {
      await incomeTransactions.update(modalTransaction.id, requestData);
      showNotification('Cập nhật giao dịch thành công');
      
      // If viewing details of this transaction, update details view
      if (selectedTransaction.value && selectedTransaction.value.id === modalTransaction.id) {
        selectedTransaction.value = await incomeTransactions.getById(modalTransaction.id);
      }
    } else {
      await incomeTransactions.create(requestData);
      showNotification('Tạo giao dịch mới thành công');
    }
    
    // Close modal before reloading data
    closeModal();
    
    // Reload current page
    await loadTransactions(pagination.currentPage);
    
  } catch (error) {
    console.error('Lỗi khi lưu giao dịch:', error);
    
    if (error.response?.status === 400) {
      showNotification(
        error.response.data.message || 'Dữ liệu không hợp lệ',
        'error'
      );
    } else {
      showNotification('Có lỗi xảy ra, vui lòng thử lại sau', 'error');
    }
  } finally {
    saving.value = false;
  }
}

// Open delete confirmation modal
function openDeleteModal(transaction) {
  transactionToDelete.value = transaction;
  showDeleteModal.value = true;
}

// Close delete confirmation modal
function closeDeleteModal() {
  showDeleteModal.value = false;
  transactionToDelete.value = null;
}

// Delete transaction
async function deleteTransaction() {
  if (!transactionToDelete.value) return;
  
  const deletedTransactionId = transactionToDelete.value.id; // Lưu ID trước khi đóng modal
  
  deleting.value = true;
  try {
    await incomeTransactions.delete(deletedTransactionId);
    
    // Close delete modal
    closeDeleteModal();
    
    showNotification('Xóa giao dịch thành công');
    
    // If viewing details of this transaction, close details
    if (selectedTransaction.value && selectedTransaction.value.id === deletedTransactionId) {
      closeDetails();
    }
    
    // Check if this is the last item on current page and not the first page
    if (transactionsList.value.length === 1 && pagination.currentPage > 0) {
      await loadTransactions(pagination.currentPage - 1);
    } else {
      // Reload current page
      await loadTransactions(pagination.currentPage);
    }
  } catch (err) {
    console.error('Lỗi khi xóa giao dịch:', err);
    showNotification('Không thể xóa giao dịch', 'error');
  } finally {
    deleting.value = false;
  }
}

// Helper function to get customer name by ID
function getCustomerName(customerId) {
  if (!customerId) return 'Không có khách hàng';
  const customer = customersList.value.find(c => c.id === customerId);
  return customer ? customer.name : 'Không xác định';
}

// Helper function to get category name by ID
function getCategoryName(categoryId) {
  if (!categoryId) return 'Không xác định';
  const category = categoriesList.value.find(c => c.id === categoryId);
  return category ? category.name : 'Không xác định';
}

// Format payment status for display
function formatPaymentStatus(status) {
  return status === 'RECEIVED' ? 'Đã nhận' : 'Chờ thanh toán';
}

// Convert transaction date to local format
function formatDate(dateString) {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', { 
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

// Convert datetime to local format
function formatDateTime(dateTimeString) {
  if (!dateTimeString) return '';
  const date = new Date(dateTimeString);
  return date.toLocaleString('vi-VN', { 
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

// Calculate total amounts
const totalAmount = computed(() => {
  return transactionsList.value.reduce((sum, transaction) => {
    return sum + parseFloat(transaction.amount);
  }, 0);
});

// Calculate received amounts
const receivedAmount = computed(() => {
  return transactionsList.value
    .filter(t => t.paymentStatus === 'RECEIVED')
    .reduce((sum, transaction) => {
      return sum + parseFloat(transaction.amount);
    }, 0);
});

// Calculate pending amounts
const pendingAmount = computed(() => {
  return transactionsList.value
    .filter(t => t.paymentStatus === 'PENDING')
    .reduce((sum, transaction) => {
      return sum + parseFloat(transaction.amount);
    }, 0);
});

// Sorting functions
function sortBy(field) {
  if (sorting.field === field) {
    sorting.direction = sorting.direction === 'asc' ? 'desc' : 'asc';
  } else {
    sorting.field = field;
    sorting.direction = 'desc';
  }
  loadTransactions(pagination.currentPage);
}

function getSortIcon(field) {
  if (sorting.field !== field) {
    return 'bi bi-arrow-down-up';
  }
  return sorting.direction === 'asc' ? 'bi bi-sort-up' : 'bi bi-sort-down';
}
</script>

<template>
  <AppLayout>
    <template #page-title>Quản lý giao dịch thu nhập</template>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý giao dịch thu nhập</h2>
          <p class="page-description">Xem và quản lý các giao dịch thu nhập</p>
        </div>
        <div class="header-actions">
          <button v-if="isAdmin" @click="openAddModal" class="btn-primary">
            <i class="bi bi-plus-lg"></i>
            Thêm giao dịch
          </button>
        </div>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container" :class="{ 'minimized': showDetails }">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel"></i>
            Tìm kiếm giao dịch
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Khách hàng</label>
              <select v-model="filters.customerId" class="form-input">
                <option :value="null">Tất cả khách hàng</option>
                <option v-for="customer in customersList" :key="customer.id" :value="customer.id">
                  {{ customer.name }}
                </option>
              </select>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Danh mục</label>
              <select v-model="filters.categoryId" class="form-input">
                <option :value="null">Tất cả danh mục</option>
                <option v-for="category in categoriesList" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Từ ngày</label>
              <input
                v-model="filters.startDate"
                type="date"
                class="form-input"
              />
            </div>
            
            <div class="filter-item">
              <label class="form-label">Đến ngày</label>
              <input
                v-model="filters.endDate"
                type="date"
                class="form-input"
              />
            </div>
            
            <div class="filter-item">
              <label class="form-label">Số tiền từ</label>
              <input
                v-model="filters.minAmount"
                type="text"
                class="form-input"
                placeholder="Số tiền tối thiểu"
              />
            </div>
            
            <div class="filter-item">
              <label class="form-label">Số tiền đến</label>
              <input
                v-model="filters.maxAmount"
                type="text"
                class="form-input"
                placeholder="Số tiền tối đa"
              />
            </div>
            
            <div class="filter-item">
              <label class="form-label">Trạng thái thanh toán</label>
              <select v-model="filters.paymentStatus" class="form-input">
                <option v-for="option in paymentStatusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Số tham chiếu</label>
              <input
                v-model="filters.referenceNo"
                type="text"
                class="form-input"
                placeholder="Nhập số tham chiếu"
              />
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilters" class="btn-outline">
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
            </button>
            <button @click.prevent="loadTransactions(0)" class="btn-primary">
              <i class="bi bi-search"></i>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
      
      <!-- Transaction List and Details Container -->
      <div class="content-layout" :class="{ 'with-details': showDetails }">
        <!-- Transactions List -->
        <div class="card transactions-list" :class="{ 'minimized': showDetails }">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-cash-coin"></i>
              Danh sách giao dịch thu nhập
            </h3>
            <span class="result-count">{{ pagination.totalItems }} giao dịch</span>
          </div>
          
          <div v-if="loading && !showDetails" class="card-empty-state">
            <div class="loading-spinner"></div>
            <p>Đang tải dữ liệu...</p>
          </div>
          
          <div v-else-if="error && !showDetails" class="card-empty-state error">
            <i class="bi bi-exclamation-circle error-icon"></i>
            <p>{{ error }}</p>
            <button @click="loadTransactions" class="btn-primary">
              <i class="bi bi-arrow-repeat"></i>
              Thử lại
            </button>
          </div>
          
          <div v-else-if="transactionsList.length === 0 && !showDetails" class="card-empty-state">
            <i class="bi bi-cash-stack empty-icon"></i>
            <p>Không tìm thấy giao dịch nào</p>
            <p class="empty-description">Vui lòng điều chỉnh bộ lọc tìm kiếm hoặc tạo giao dịch mới</p>
          </div>
          
          <div v-else class="table-responsive">
            <table class="data-table">
              <thead>
                <tr>
                  <th @click="sortBy('transactionDate')" class="sortable-column">
                    Ngày GD
                    <i :class="getSortIcon('transactionDate')"></i>
                  </th>
                  <th @click="sortBy('amount')" class="sortable-column">
                    Số tiền
                    <i :class="getSortIcon('amount')"></i>
                  </th>
                  <th @click="sortBy('categoryName')" class="sortable-column">
                    Danh mục
                    <i :class="getSortIcon('categoryName')"></i>
                  </th>
                  <th>Khách hàng</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="transaction in transactionsList" 
                  :key="transaction.id" 
                  @click="showTransactionDetails(transaction)"
                  class="transaction-row"
                  :class="{ 'active': selectedTransaction && selectedTransaction.id === transaction.id }"
                >
                  <td>{{ formatDate(transaction.transactionDate) }}</td>
                  <td class="amount-cell">{{ formatAmountDisplay(transaction.amount) }}</td>
                  <td>{{ transaction.categoryName }}</td>
                  <td>{{ transaction.customerName || 'Không có' }}</td>
                  <td>
                    <span :class="['status-badge', transaction.paymentStatus === 'RECEIVED' ? 'active' : 'pending']">
                      <i :class="[transaction.paymentStatus === 'RECEIVED' ? 'bi bi-check-circle' : 'bi bi-hourglass-split']"></i>
                      {{ formatPaymentStatus(transaction.paymentStatus) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- Pagination Controls -->
          <div v-if="transactionsList.length > 0" class="pagination-container">
            <div class="pagination-info">
              Hiển thị {{ transactionsList.length }} trên tổng số {{ pagination.totalItems }} giao dịch
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
                :disabled="pagination.currentPage === pagination.totalPages - 1 || pagination.totalPages === 0"
                class="pagination-btn"
                title="Trang sau"
              >
                <i class="bi bi-chevron-right"></i>
              </button>
              
              <button 
                @click.prevent="goToPage(pagination.totalPages - 1)" 
                :disabled="pagination.currentPage === pagination.totalPages - 1 || pagination.totalPages === 0"
                class="pagination-btn"
                title="Trang cuối"
              >
                <i class="bi bi-chevron-double-right"></i>
              </button>
            </div>
          </div>
          
          <!-- Transaction Summary -->
          <div v-if="transactionsList.length > 0" class="transaction-summary">
            <div class="summary-item">
              <span class="summary-label">Tổng số tiền:</span> 
              <span class="summary-value">{{ formatAmountDisplay(totalAmount) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">Đã nhận:</span>
              <span class="summary-value received">{{ formatAmountDisplay(receivedAmount) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">Chờ thanh toán:</span>
              <span class="summary-value pending">{{ formatAmountDisplay(pendingAmount) }}</span>
            </div>
          </div>
        </div>
        
        <!-- Transaction Details Modal -->
        <div v-if="showDetails" class="modal-overlay details-modal-overlay">
          <div class="modal-container details-modal-container">
            <div class="modal-header">
              <h3 class="modal-title">
                <i class="bi bi-info-circle"></i>
                Chi tiết giao dịch
              </h3>
              <button @click="closeDetails" class="btn-close" title="Đóng chi tiết">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>
            
            <div v-if="loading" class="modal-content">
              <div class="card-empty-state">
                <div class="loading-spinner"></div>
                <p>Đang tải thông tin chi tiết...</p>
              </div>
            </div>
            
            <div v-else-if="selectedTransaction" class="modal-content">
              <div class="detail-header">
                <div class="transaction-avatar">
                  <i class="bi bi-cash-coin"></i>
                </div>
                <h3 class="transaction-amount">{{ formatAmountDisplay(selectedTransaction.amount) }}</h3>
                <p class="transaction-date">{{ formatDate(selectedTransaction.transactionDate) }}</p>
              </div>
              
              <div class="detail-section">
                <h4 class="detail-section-title">Thông tin giao dịch</h4>
                
                <div class="detail-row">
                  <div class="detail-label">
                    <i class="bi bi-tag"></i>
                    Danh mục:
                  </div>
                  <div class="detail-value">{{ selectedTransaction.categoryName }}</div>
                </div>
                
                <div class="detail-row">
                  <div class="detail-label">
                    <i class="bi bi-person"></i>
                    Khách hàng:
                  </div>
                  <div class="detail-value">{{ selectedTransaction.customerName || 'Không có' }}</div>
                </div>
                
                <div class="detail-row">
                  <div class="detail-label">
                    <i class="bi bi-calendar-check"></i>
                    Trạng thái:
                  </div>
                  <div class="detail-value">
                    <span :class="['status-badge', selectedTransaction.paymentStatus === 'RECEIVED' ? 'active' : 'pending']">
                      <i :class="[selectedTransaction.paymentStatus === 'RECEIVED' ? 'bi bi-check-circle' : 'bi bi-hourglass-split']"></i>
                      {{ formatPaymentStatus(selectedTransaction.paymentStatus) }}
                    </span>
                  </div>
                </div>
                
                <div class="detail-row">
                  <div class="detail-label">
                    <i class="bi bi-hash"></i>
                    Số tham chiếu:
                  </div>
                  <div class="detail-value">{{ selectedTransaction.referenceNo || 'Không có' }}</div>
                </div>
                
                <div class="detail-row">
                  <div class="detail-label">
                    <i class="bi bi-card-text"></i>
                    Mô tả:
                  </div>
                  <div class="detail-value description-value">{{ selectedTransaction.description || 'Không có mô tả' }}</div>
                </div>
              </div>
            </div>
            
            <div v-if="isAdmin && selectedTransaction" class="modal-actions">
              <button @click="openEditModal(selectedTransaction)" class="btn-primary">
                <i class="bi bi-pencil"></i>
                Sửa
              </button>
              <button @click="openDeleteModal(selectedTransaction)" class="btn-outline btn-danger-outline">
                <i class="bi bi-trash"></i>
                Xóa
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Add/Edit Transaction Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">{{ editMode ? 'Chỉnh sửa giao dịch' : 'Thêm giao dịch' }}</h3>
          <button @click="closeModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-content">
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">Danh mục</label>
              <select v-model="modalTransaction.categoryId" class="form-input" name="categoryId">
                <option :value="null">Chọn danh mục</option>
                <option v-for="category in categoriesList" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
              <span v-if="modalErrors.categoryId" class="form-error">{{ modalErrors.categoryId }}</span>
            </div>
            
            <div class="form-item">
              <label class="form-label">Khách hàng</label>
              <select v-model="modalTransaction.customerId" class="form-input">
                <option :value="null">Chọn khách hàng</option>
                <option v-for="customer in customersList" :key="customer.id" :value="customer.id">
                  {{ customer.name }}
                </option>
              </select>
            </div>
            
            <div class="form-item">
              <label class="form-label">Ngày giao dịch</label>
              <input
                v-model="modalTransaction.transactionDate"
                type="date"
                class="form-input"
              />
              <span v-if="modalErrors.transactionDate" class="form-error">{{ modalErrors.transactionDate }}</span>
            </div>
            
            <div class="form-item">
              <label class="form-label">Số tiền</label>
              <input
                v-model="modalTransaction.amount"
                type="text"
                class="form-input"
                @input="validateAmount"
                placeholder="Nhập số tiền"
              />
              <span v-if="modalErrors.amount" class="form-error">{{ modalErrors.amount }}</span>
            </div>
            
            <div class="form-item">
              <label class="form-label">Trạng thái thanh toán</label>
              <select v-model="modalTransaction.paymentStatus" class="form-input">
                <option value="PENDING">Chờ thanh toán</option>
                <option value="RECEIVED">Đã nhận</option>
              </select>
            </div>
            
            <div class="form-item">
              <label class="form-label">Số tham chiếu</label>
              <input
                v-model="modalTransaction.referenceNo"
                type="text"
                class="form-input"
                placeholder="Nhập số tham chiếu"
              />
              <span v-if="modalErrors.referenceNo" class="form-error">{{ modalErrors.referenceNo }}</span>
            </div>
            
            <div class="form-item">
              <label class="form-label">Mô tả</label>
              <textarea
                v-model="modalTransaction.description"
                class="form-input"
                placeholder="Nhập mô tả"
              ></textarea>
            </div>
          </div>
        </div>
        
        <div class="modal-actions">
          <button @click="closeModal" class="btn-outline">
            <i class="bi bi-x-lg"></i>
            Hủy bỏ
          </button>
          <button @click="saveTransaction" class="btn-primary" :disabled="saving">
            <i v-if="saving" class="bi bi-arrow-repeat loading-icon"></i>
            <i v-else class="bi bi-save"></i>
            Lưu
          </button>
        </div>
      </div>
    </div>
    
    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Xóa giao dịch</h3>
          <button @click="closeDeleteModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-content">
          <p>Bạn có chắc chắn muốn xóa giao dịch này không?</p>
          <p class="modal-warning">Hành động này không thể hoàn tác.</p>
        </div>
        
        <div class="modal-actions">
          <button @click="closeDeleteModal" class="btn-outline">
            <i class="bi bi-x-lg"></i>
            Hủy bỏ
          </button>
          <button @click="deleteTransaction" class="btn-danger" :disabled="deleting">
            <i v-if="deleting" class="bi bi-arrow-repeat loading-icon"></i>
            <i v-else class="bi bi-trash"></i>
            Xóa
          </button>
        </div>
      </div>
    </div>
    
    <!-- Notification -->
    <div v-if="notification.show" class="notification" :class="notification.type">
      <i :class="notification.type === 'success' ? 'bi bi-check-circle' : 'bi bi-exclamation-circle'"></i>
      <span>{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-container {
  padding: 2rem;
  max-width: 1200px; /* Thêm độ rộng tối đa */
  margin: 0 auto; /* Căn giữa container */
}

/* Sửa lỗi styling cho filter container */
.filter-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  overflow: hidden; /* Để tránh lỗi border-radius */
}

.filter-header {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem; /* Thêm padding trái phải */
  background-color: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.filter-content {
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
}

/* Sửa styling cho filter grid responsive */
.filter-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr; /* Mặc định 1 cột trên mobile */
}

@media (min-width: 769px) and (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr); /* Hai cột trên tablet */
  }
}

@media (min-width: 1201px) {
  .filter-grid {
    grid-template-columns: repeat(4, 1fr); /* Bốn cột trên desktop */
  }
}

/* Sửa lỗi trong hiển thị form-input và select */
.form-input, select.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: white;
  font-size: 0.875rem;
  color: #1f2937;
  transition: all 0.2s ease;
}

.form-input:focus, select.form-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.2);
}

.form-input.error {
  border-color: #ef4444;
}

/* Sửa lại thiết kế cho content-layout khi có details */
.content-layout {
  display: flex;
  gap: 2rem;
  flex-direction: column;
  width: 100%;
}

/* Chỉnh sửa transactions-list để đảm bảo nó chiếm đủ không gian */
.transactions-list {
  width: 100%; /* Chiếm toàn bộ không gian có sẵn */
  flex: 1;
  min-width: 0; /* Quan trọng để tránh flex child overflow */
}

/* Sửa thiết kế content-layout khi có details */
@media (min-width: 992px) {
  .content-layout {
    flex-direction: row; /* Trên desktop hiển thị theo hàng ngang */
    align-items: flex-start; /* Đảm bảo các thành phần căn trên */
  }
}

/* Sửa lại table-responsive để đảm bảo bảng không bị overflow */
.table-responsive {
  width: 100%;
  overflow-x: auto;
  min-width: 0; /* Quan trọng để tránh overflow */
}

/* Sửa lỗi width trong data-table */
.data-table {
  width: 100%;
  min-width: 100%;
  table-layout: fixed; /* Giúp kiểm soát độ rộng các cột tốt hơn */
  border-collapse: separate;
  border-spacing: 0;
}

/* Thiết lập độ rộng của các cột trong bảng */
.data-table th:nth-child(1), 
.data-table td:nth-child(1) {
  width: 12%; /* Ngày giao dịch */
}

.data-table th:nth-child(2), 
.data-table td:nth-child(2) {
  width: 15%; /* Số tiền */
}

.data-table th:nth-child(3), 
.data-table td:nth-child(3) {
  width: 20%; /* Danh mục */
}

.data-table th:nth-child(4), 
.data-table td:nth-child(4) {
  width: 25%; /* Khách hàng */
}

.data-table th:nth-child(5), 
.data-table td:nth-child(5) {
  width: 18%; /* Trạng thái */
}

.action-column {
  width: 10%; /* Thao tác */
}

/* Sửa notification để tránh lỗi z-index và positioning */
.notification {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  min-width: 300px;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 9999; /* Đảm bảo hiển thị trên cùng */
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background-color: white;
  animation: slide-in 0.3s ease-out;
}

@keyframes slide-in {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Sửa phần còn lại của CSS giữ nguyên */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-title {
  color: #111827;
  margin: 0;
}

.page-description {
  color: #6b7280;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-primary i {
  margin-right: 0.5rem;
}

.btn-outline {
  background-color: white;
  color: #3b82f6;
  border: 1px solid #3b82f6;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-outline i {
  margin-right: 0.5rem;
}

.btn-danger {
  background-color: #ef4444;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-danger i {
  margin-right: 0.5rem;
}

.btn-danger-outline {
  background-color: white;
  color: #ef4444;
  border: 1px solid #ef4444;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-danger-outline i {
  margin-right: 0.5rem;
}

.btn-close {
  background-color: transparent;
  color: #6b7280;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.btn-close i {
  font-size: 1.25rem;
}

.filter-container.minimized {
  display: none;
}

.filter-header h3 {
  margin: 0;
  color: #111827;
}

.filter-content {
  display: flex;
  flex-direction: column;
}

.filter-grid {
  display: grid;
  gap: 1rem;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.filter-item label {
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.content-layout.with-details .transactions-list {
  flex: 1;
}

.content-layout.with-details .transaction-details {
  flex: 1;
}

.card {
  background-color: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  width: 100%; /* Đảm bảo card chiếm toàn bộ không gian có sẵn */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.card-title {
  color: #111827;
  margin: 0;
}

.result-count {
  color: #6b7280;
}

.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  text-align: center;
}

.card-empty-state.error {
  color: #ef4444;
}

.card-empty-state .loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 4px solid #d1d5db;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.card-empty-state .error-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.card-empty-state .empty-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.card-empty-state .empty-description {
  color: #6b7280;
}

.data-table th,
.data-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #d1d5db;
}

.data-table td {
  color: #111827;
}

.data-table .amount-cell {
  text-align: right;
}

.data-table .action-column {
  width: 100px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
}

.status-badge.active {
  background-color: #d1fae5;
  color: #10b981;
}

.status-badge.pending {
  background-color: #fef3c7;
  color: #f59e0b;
}

.status-badge i {
  margin-right: 0.25rem;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.pagination-info {
  color: #6b7280;
}

.pagination-controls {
  display: flex;
  align-items: center;
}

.pagination-btn {
  background-color: white;
  color: #6b7280;
  border: 1px solid #d1d5db;
  padding: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.pagination-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.pagination-btn i {
  font-size: 1rem;
}

.pagination-text {
  color: #6b7280;
  margin: 0 0.5rem;
}

.transaction-summary {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
}

.summary-item {
  display: flex;
  align-items: center;
}

.summary-label {
  color: #6b7280;
  margin-right: 0.5rem;
}

.summary-value {
  color: #111827;
}

.summary-value.received {
  color: #10b981;
}

.summary-value.pending {
  color: #f59e0b;
}

.transaction-details {
  flex: 1;
}

.transaction-details .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.transaction-details .card-title {
  color: #111827;
  margin: 0;
}

.transaction-details .btn-close {
  background-color: transparent;
  color: #6b7280;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.transaction-details .btn-close i {
  font-size: 1.25rem;
}

.transaction-details-content {
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2rem;
  text-align: center;
}

.transaction-avatar {
  background-color: #f3f4f6;
  color: #3b82f6;
  border-radius: 50%;
  width: 4rem;
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.transaction-avatar i {
  font-size: 2rem;
}

.transaction-amount {
  font-size: 1.75rem; /* Tăng kích thước chữ */
  font-weight: 700; /* Đậm hơn */
  color: #111827;
  margin: 0 0 0.25rem 0;
}

.transaction-date {
  color: #6b7280;
  margin: 0;
}

.detail-section {
  margin-bottom: 0; /* Giảm khoảng cách dưới vì đã bỏ phần thông tin hệ thống */
  padding-bottom: 0.5rem;
}

.detail-section-title {
  color: #111827;
  margin: 0 0 0.5rem 0;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.detail-label {
  color: #6b7280;
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
}

.detail-label i {
  margin-right: 0.25rem;
}

.detail-value {
  color: #111827;
  font-weight: 500; /* Làm đậm giá trị */
}

.detail-actions {
  display: flex;
  flex-direction: row; /* Đảm bảo nằm ngang */
  justify-content: flex-start; /* Căn trái */
  gap: 0.75rem; /* Khoảng cách giữa các nút */
  margin-top: 1.5rem; /* Thêm khoảng cách phía trên */
  padding-top: 1rem; /* Thêm padding phía trên */
  border-top: 1px solid #e5e7eb; /* Thêm đường ngăn cách */
  width: 100%; /* Đảm bảo chiếm toàn bộ chiều rộng */
}

/* Điều chỉnh kích thước nút trong detail-actions */
.detail-actions button {
  min-width: 80px; /* Giảm kích thước nút */
  padding: 0.3rem 0.5rem; /* Giảm padding */
  font-size: 0.75rem; /* Giảm kích thước chữ */
  justify-content: center; /* Căn giữa nội dung button */
  height: 30px; /* Đặt chiều cao cố định */
  flex: 0 0 auto; /* Ngăn không cho nút co giãn */
}

.detail-actions button i {
  font-size: 0.75rem; /* Giảm kích thước icon */
  margin-right: 0.25rem; /* Giảm khoảng cách giữa icon và text */
}

/* Chỉ sử dụng flex-direction: column trên màn hình rất nhỏ */
@media (max-width: 359px) {
  .detail-actions {
    flex-direction: column; /* Stack buttons on very small screens */
    width: 100%;
  }
  
  .detail-actions button {
    width: 100%; /* Full width buttons on very small screens */
    margin-bottom: 0.5rem;
  }
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 650px;
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 2;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  color: #111827;
}

.modal-content {
  padding: 1.5rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 1.5rem;
  background-color: #f8fafc;
  border-top: 1px solid #e5e7eb;
  gap: 0.75rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr; /* Default is single column */
  gap: 1rem;
}

@media (min-width: 640px) {
  .form-grid {
    grid-template-columns: repeat(2, 1fr); /* Two columns on wider screens */
  }
}

/* Make description textarea span full width */
.form-grid .form-item:last-child {
  grid-column: 1 / -1;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-label {
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.form-error {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.modal-warning {
  color: #ef4444;
  font-size: 0.875rem;
  font-style: italic;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.notification.success {
  border-left: 4px solid #10b981;
}

.notification.error {
  border-left: 4px solid #ef4444;
}

.notification i {
  font-size: 1.5rem;
}

.notification span {
  color: #111827;
}

@media (max-width: 767px) {
  .content-layout {
    flex-direction: column;
  }
  
  .transactions-list.minimized {
    display: none;
  }
  
  .transaction-details {
    width: 100%;
  }
  
  .transaction-summary {
    flex-direction: column;
    gap: 0.75rem;
  }
  
  .summary-item {
    justify-content: space-between;
  }
  
  .pagination-container {
    flex-direction: column;
    gap: 1rem;
  }
}

/* Thêm style cho modal chi tiết */
.details-modal-container {
  max-width: 550px; /* Giảm độ rộng vì ít thông tin hơn */
}

.details-modal-overlay {
  z-index: 999; /* Đảm bảo hiển thị dưới modal thêm/sửa */
}

.detail-header {
  padding: 1rem 0;
  margin-bottom: 1.5rem;
  align-items: center;
  text-align: center;
}

/* Đảm bảo mô tả dài không bị cắt */
.description-value {
  white-space: pre-wrap; 
  word-break: break-word;
}

/* Sửa hiển thị của các dòng thông tin */
.detail-row {
  display: flex;
  flex-direction: column; /* Hiển thị label trên, value dưới trên mobile */
  margin-bottom: 1rem;
}

.detail-label {
  color: #6b7280;
  margin-bottom: 0.25rem;
  font-weight: 500;
}

.detail-value {
  color: #111827;
}

/* Responsive cho desktop */
@media (min-width: 768px) {
  .detail-row {
    flex-direction: row; /* Hiển thị label và value trên cùng một dòng */
    align-items: flex-start;
  }
  
  .detail-label {
    width: 150px; /* Chiều rộng cố định cho phần label */
    padding-right: 1rem;
  }
  
  .detail-value {
    flex: 1; /* Chiếm hết phần còn lại */
  }
}

/* Sửa nút trong modal-actions */
.modal-actions button {
  min-width: 100px;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.action-buttons .btn-outline.btn-icon {
  width: 28px; /* Giảm kích thước */
  height: 28px; /* Giảm kích thước */
  padding: 0;
  margin: 0 2px;
}

.action-buttons .btn-icon i {
  font-size: 0.75rem; /* Icon nhỏ hơn */
  margin: 0;
}
</style>
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
    
    // Cập nhật lại cách truy cập thông tin phân trang
    if (result.page) {
      pagination.currentPage = result.page.page;
      pagination.totalPages = result.page.totalPages;
      pagination.totalItems = result.page.totalElement;
    }
    
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
              <select v-model="filters.customerId" class="form-select">
                <option :value="null">Tất cả khách hàng</option>
                <option v-for="customer in customersList" :key="customer.id" :value="customer.id">
                  {{ customer.name }}
                </option>
              </select>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Danh mục</label>
              <select v-model="filters.categoryId" class="form-select">
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
              <select v-model="filters.paymentStatus" class="form-select">
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
                  <th class="action-column text-center">Thao tác</th>
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
                  <td class="action-column text-center">
                    <div class="action-buttons">
                      <button 
                        @click.stop="showTransactionDetails(transaction)"
                        class="btn-icon view-btn"
                        title="Xem chi tiết"
                      >
                        <i class="bi bi-eye"></i>
                      </button>
                      <button 
                        @click.stop="openEditModal(transaction)"
                        class="btn-icon edit-btn"
                        title="Chỉnh sửa"
                      >
                        <i class="bi bi-pencil-square"></i>
                      </button>
                      <button 
                        @click.stop="openDeleteModal(transaction)"
                        class="btn-icon delete-btn"
                        title="Xóa"
                      >
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
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
        <div v-if="showDetails" class="modal-overlay">
          <div class="modal-container">
            <div class="modal-header">
              <h3 class="modal-title">
                <i class="bi bi-info-circle"></i>
                Chi tiết giao dịch
              </h3>
              <button @click="closeDetails" class="btn-close" title="Đóng chi tiết">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>
            
            <div v-if="loading" class="modal-body">
              <div class="card-empty-state">
                <div class="loading-spinner"></div>
                <p>Đang tải thông tin chi tiết...</p>
              </div>
            </div>
            
            <div v-else-if="selectedTransaction" class="modal-body">
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
              
              <div v-if="isAdmin && selectedTransaction" class="modal-actions">
                <button @click="openEditModal(selectedTransaction)" class="btn-primary">
                  <i class="bi bi-pencil-square"></i>
                  Sửa
                </button>
                <button @click="openDeleteModal(selectedTransaction)" class="btn-danger">
                  <i class="bi bi-trash"></i>
                  Xóa
                </button>
              </div>
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
        
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label required">Danh mục</label>
              <select v-model="modalTransaction.categoryId" class="form-select" name="categoryId">
                <option :value="null">Chọn danh mục</option>
                <option v-for="category in categoriesList" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
              <span v-if="modalErrors.categoryId" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.categoryId }}
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label">Khách hàng</label>
              <select v-model="modalTransaction.customerId" class="form-select">
                <option :value="null">Chọn khách hàng</option>
                <option v-for="customer in customersList" :key="customer.id" :value="customer.id">
                  {{ customer.name }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label required">Ngày giao dịch</label>
              <input
                v-model="modalTransaction.transactionDate"
                type="date"
                class="form-input"
              />
              <span v-if="modalErrors.transactionDate" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.transactionDate }}
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label required">Số tiền</label>
              <input
                v-model="modalTransaction.amount"
                type="text"
                class="form-input"
                @input="validateAmount"
                placeholder="Nhập số tiền"
              />
              <span v-if="modalErrors.amount" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.amount }}
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label">Trạng thái thanh toán</label>
              <select v-model="modalTransaction.paymentStatus" class="form-select">
                <option value="PENDING">Chờ thanh toán</option>
                <option value="RECEIVED">Đã nhận</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Số tham chiếu</label>
              <input
                v-model="modalTransaction.referenceNo"
                type="text"
                class="form-input"
                placeholder="Nhập số tham chiếu"
              />
              <span v-if="modalErrors.referenceNo" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.referenceNo }}
              </span>
            </div>
            
            <div class="form-group" style="grid-column: span 2;">
              <label class="form-label">Mô tả</label>
              <textarea
                v-model="modalTransaction.description"
                class="form-textarea"
                placeholder="Nhập mô tả"
                rows="3"
              ></textarea>
            </div>
          </div>
          
          <div class="modal-actions">
            <button @click="closeModal" class="btn-outline">
              <i class="bi bi-x-lg"></i>
              Hủy bỏ
            </button>
            <button @click="saveTransaction" class="btn-primary" :disabled="saving">
              <i v-if="saving" class="bi bi-arrow-repeat spinner"></i>
              <i v-else class="bi bi-save"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container modal-confirm">
        <div class="modal-header">
          <h3 class="modal-title">Xác nhận xóa</h3>
          <button @click="closeDeleteModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-body text-center">
          <div class="icon-warning">
            <i class="bi bi-exclamation-triangle-fill"></i>
          </div>
          <h4 class="confirm-title">Bạn có chắc chắn muốn xóa?</h4>
          <p class="confirm-message">
            Giao dịch này sẽ bị xóa vĩnh viễn.
            <br>Hành động này không thể hoàn tác.
          </p>
          
          <div class="modal-actions">
            <button @click="closeDeleteModal" class="btn-outline">
              <i class="bi bi-x-lg"></i>
              Hủy bỏ
            </button>
            <button @click="deleteTransaction" class="btn-danger" :disabled="deleting">
              <i v-if="deleting" class="bi bi-arrow-repeat spinner"></i>
              <i v-else class="bi bi-trash"></i>
              {{ deleting ? 'Đang xóa...' : 'Xóa' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Toast Notification -->
    <div 
      v-if="notification.show"
      :class="['toast-notification', notification.type]"
    >
      <i :class="[
        notification.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
      ]"></i>
      <span>{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
/* Component-specific styles only */
/* All common styles have been moved to assets/styles/common.css */

/* Modal layout customizations */
.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

@media (min-width: 640px) {
  .form-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Specific column widths for this view's table */
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

/* Transaction detail specific styles */
.transaction-avatar {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background-color: #e0e7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.transaction-avatar i {
  font-size: 2rem;
  color: #4f46e5;
}

.transaction-amount {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.25rem;
  text-align: center;
}

.transaction-date {
  color: #64748b;
  margin: 0;
  text-align: center;
}

.detail-section {
  margin: 1.5rem 0;
}

.detail-section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  margin-bottom: 0.75rem;
}

.detail-label {
  min-width: 8rem;
  color: #6b7280;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.detail-value {
  flex: 1;
  color: #1e293b;
}

.detail-label i {
  color: #6366f1;
}

.description-value {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
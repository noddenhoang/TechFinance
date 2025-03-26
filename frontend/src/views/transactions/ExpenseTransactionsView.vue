<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { formatCurrency } from '../../utils/formatters';
import { useAuthStore } from '../../stores/auth';

// Import các API service cần thiết
import { expenseTransactions } from '../../api/expenseTransactions';  
import { suppliers } from '../../api/suppliers';
import { expenseCategories } from '../../api/expenseCategories';

// Data state
const transactionsList = ref([]);
const selectedTransaction = ref(null);
const loading = ref(true);
const error = ref(null);
const showDetails = ref(false);

// Data for dropdowns
const suppliersList = ref([]);
const categoriesList = ref([]);
const loadingSuppliers = ref(false);
const loadingCategories = ref(false);

// Auth store để kiểm tra quyền admin
const auth = useAuthStore();

// Kiểm tra quyền admin
const isAdmin = computed(() => {
  // Debug dữ liệu auth để xem cấu trúc thực tế
  console.log('Auth user:', auth.user);
  
  // Nếu không có user, chắc chắn không phải admin
  if (!auth.user) return false;
  
  // Kiểm tra tất cả các cấu trúc phổ biến
  
  // Kiểm tra từ thuộc tính role trực tiếp trên user (cấu trúc đơn giản nhất)
  if (auth.user.role === 'ROLE_ADMIN' || auth.user.role === 'admin') {
    return true;
  }
  
  // Kiểm tra authorities là mảng string
  if (Array.isArray(auth.user.authorities)) {
    // Trường hợp mảng string
    if (auth.user.authorities.includes('ROLE_ADMIN')) {
      return true;
    }
    
    // Trường hợp mảng object
    if (auth.user.authorities.some(a => 
        (typeof a === 'object' && (a.authority === 'ROLE_ADMIN' || a.role === 'ROLE_ADMIN'))
      )) {
      return true;
    }
  }
  
  // Kiểm tra roles là mảng
  if (Array.isArray(auth.user.roles)) {
    if (auth.user.roles.includes('ROLE_ADMIN') || auth.user.roles.includes('admin')) {
      return true;
    }
  }
  
  // Kiểm tra trường hợp có một authority object chứa authority array
  if (auth.user.authority && Array.isArray(auth.user.authority.authorities)) {
    if (auth.user.authority.authorities.includes('ROLE_ADMIN')) {
      return true;
    }
  }
  
  // Luôn trả về true trong khi dev (bỏ comment dòng dưới đây để tạm thời bypass kiểm tra)
  // return true;
  
  // Không tìm thấy quyền admin trong tất cả các cấu trúc
  return false;
});

// States for Add/Edit modal
const showModal = ref(false);
const editMode = ref(false);
const saving = ref(false);
const modalTransaction = reactive({
  id: null,
  categoryId: null,
  supplierId: null,
  transactionDate: new Date().toISOString().slice(0, 10), // Today in YYYY-MM-DD format
  amount: '',
  paymentStatus: 'UNPAID',
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
  supplierId: null,
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
  { value: 'PAID', label: 'Đã thanh toán' },
  { value: 'UNPAID', label: 'Chưa thanh toán' }
];

// Load transactions on component mount
onMounted(async () => {
  await Promise.all([
    loadTransactions(),
    loadSuppliersList(),
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
    
    const result = await expenseTransactions.getAll(
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
    error.value = 'Không thể tải danh sách giao dịch chi phí. Vui lòng thử lại sau.';
    console.error('Error loading transactions:', err);
  } finally {
    loading.value = false;
  }
}

// Load suppliers for dropdown
async function loadSuppliersList() {
  loadingSuppliers.value = true;
  try {
    // Chỉ lấy suppliers có trạng thái active
    const response = await suppliers.getAll({isActive: true}, 0, 1000);
    suppliersList.value = response.content || [];
  } catch (err) {
    console.error('Error loading suppliers:', err);
  } finally {
    loadingSuppliers.value = false;
  }
}

// Load categories for dropdown
async function loadCategoriesList() {
  loadingCategories.value = true;
  try {
    // Use getAll with empty filters to get all active categories
    const response = await expenseCategories.getAll({isActive: true}, 0, 1000);
    categoriesList.value = response.content || [];
  } catch (err) {
    console.error('Error loading expense categories:', err);
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
  
  filters.supplierId = null;
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

// Show transaction details
async function showTransactionDetails(transaction) {
  try {
    loading.value = true;
    // Fetch detailed info if needed
    const detailedTransaction = await expenseTransactions.getById(transaction.id);
    selectedTransaction.value = detailedTransaction;
    showDetails.value = true; // Hiện modal thay vì thay đổi layout
  } catch (err) {
    console.error('Error fetching transaction details:', err);
    error.value = 'Không thể tải thông tin chi tiết giao dịch.';
  } finally {
    loading.value = false;
  }
}

// Close details
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
  modalTransaction.categoryId = "";
  modalTransaction.supplierId = null;
  modalTransaction.transactionDate = new Date().toISOString().slice(0, 10);
  modalTransaction.amount = '';
  modalTransaction.paymentStatus = 'UNPAID';
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
  modalTransaction.supplierId = transaction.supplierId;
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

// Validate all form fields
function validateForm() {
  let isValid = true;
  
  // Reset all errors first
  Object.keys(modalErrors).forEach(key => modalErrors[key] = '');
  
  // Check required fields
  if (!modalTransaction.categoryId) {
    modalErrors.categoryId = 'Vui lòng chọn danh mục';
    isValid = false;
  }
  
  if (!modalTransaction.transactionDate) {
    modalErrors.transactionDate = 'Vui lòng chọn ngày giao dịch';
    isValid = false;
  }
  
  if (!modalTransaction.amount) {
    modalErrors.amount = 'Vui lòng nhập số tiền';
    isValid = false;
  } else if (parseFloat(modalTransaction.amount) <= 0) {
    modalErrors.amount = 'Số tiền phải lớn hơn 0';
    isValid = false;
  }
  
  if (!modalTransaction.paymentStatus) {
    modalErrors.paymentStatus = 'Vui lòng chọn trạng thái thanh toán';
    isValid = false;
  }
  
  return isValid;
}

// Save transaction (create or update)
async function saveTransaction() {
  // Reset errors
  Object.keys(modalErrors).forEach(key => modalErrors[key] = '');
  
  // Validate
  let isValid = validateForm();
  
  if (!isValid) return;
  
  saving.value = true;
  try {
    const requestData = {
      categoryId: parseInt(modalTransaction.categoryId),
      supplierId: modalTransaction.supplierId ? parseInt(modalTransaction.supplierId) : null,
      transactionDate: modalTransaction.transactionDate,
      amount: parseFloat(modalTransaction.amount),
      paymentStatus: modalTransaction.paymentStatus,
      description: modalTransaction.description,
      referenceNo: modalTransaction.referenceNo
    };
    
    if (editMode.value) {
      await expenseTransactions.update(modalTransaction.id, requestData);
      showNotification('Cập nhật giao dịch thành công');
      
      // If viewing details of this transaction, update details view
      if (selectedTransaction.value && selectedTransaction.value.id === modalTransaction.id) {
        selectedTransaction.value = await expenseTransactions.getById(modalTransaction.id);
      }
    } else {
      await expenseTransactions.create(requestData);
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
    await expenseTransactions.delete(deletedTransactionId);
    
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

// Helper function to get supplier name by ID
function getSupplierName(supplierId) {
  if (!supplierId) return 'Không có nhà cung cấp';
  const supplier = suppliersList.value.find(s => s.id === supplierId);
  return supplier ? supplier.name : 'Không xác định';
}

// Helper function to get category name by ID
function getCategoryName(categoryId) {
  if (!categoryId) return 'Không xác định';
  const category = categoriesList.value.find(c => c.id === categoryId);
  return category ? category.name : 'Không xác định';
}

// Format payment status for display
function formatPaymentStatus(status) {
  return status === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán';
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

// Calculate paid amounts
const paidAmount = computed(() => {
  return transactionsList.value
    .filter(t => t.paymentStatus === 'PAID')
    .reduce((sum, transaction) => {
      return sum + parseFloat(transaction.amount);
    }, 0);
});

// Calculate unpaid amounts
const unpaidAmount = computed(() => {
  return transactionsList.value
    .filter(t => t.paymentStatus === 'UNPAID')
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

// Helper functions for supplier tooltips
function getSupplierDetails(supplierId) {
  if (!supplierId) return null;
  
  const supplier = suppliersList.value.find(s => s.id === supplierId);
  return supplier || null;
}

// Hàm tạo nội dung tooltip cho nhà cung cấp
function getSupplierTooltip(supplier) {
  if (!supplier) return '';
  
  let tooltip = `${supplier.name}`;
  if (supplier.phone) tooltip += `\nSĐT: ${supplier.phone}`;
  if (supplier.email) tooltip += `\nEmail: ${supplier.email}`;
  
  return tooltip;
}

// Add a style block for required field indicators and define required fields
const requiredFields = ['categoryId', 'amount', 'transactionDate'];

// Add a computed property for safe access to selectedTransaction
const safeSelectedTransaction = computed(() => {
  return selectedTransaction.value || {};
});
</script>

<template>
  <AppLayout>
    <!-- Toast Notification -->
    <div 
      v-if="notification.show"
      :class="[
        'toast-notification',
        notification.type === 'success' ? 'success' : 'error'
      ]"
    >
      <i :class="[
        notification.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
      ]"></i>
      <span>{{ notification.message }}</span>
    </div>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý giao dịch chi phí</h2>
          <p class="page-description">Xem và quản lý các giao dịch chi phí</p>
        </div>
        <div class="header-actions">
          <button v-if="isAdmin" @click="openAddModal" class="btn-primary">
            <i class="bi bi-plus-lg"></i>
            Thêm giao dịch
          </button>
        </div>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel"></i>
            Tìm kiếm giao dịch
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Nhà cung cấp</label>
              <select v-model="filters.supplierId" class="form-select">
                <option :value="null">Tất cả nhà cung cấp</option>
                <option v-for="supplier in suppliersList" :key="supplier.id" :value="supplier.id">
                  {{ supplier.name }}
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
      
      <!-- Transaction List -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="bi bi-cash-coin"></i>
            Danh sách giao dịch chi phí
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
                <th @click="sortBy('transactionDate')" class="sortable">
                  Ngày GD
                  <i :class="getSortIcon('transactionDate')" class="sort-icon"></i>
                </th>
                <th @click="sortBy('amount')" class="sortable">
                  Số tiền
                  <i :class="getSortIcon('amount')" class="sort-icon"></i>
                </th>
                <th @click="sortBy('categoryName')" class="sortable">
                  Danh mục
                  <i :class="getSortIcon('categoryName')" class="sort-icon"></i>
                </th>
                <th>Nhà cung cấp</th>
                <th>Trạng thái</th>
                <th class="action-column">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="transaction in transactionsList" 
                :key="transaction.id" 
                @click="showTransactionDetails(transaction)"
              >
                <td>{{ formatDate(transaction.transactionDate) }}</td>
                <td class="text-right font-medium">{{ formatAmountDisplay(transaction.amount) }}</td>
                <td>{{ transaction.categoryName }}</td>
                <td>
                  <div class="customer-info">
                    {{ transaction.supplierName || 'Không có' }}
                    <div v-if="transaction.supplierId">
                    </div>
                  </div>
                </td>
                <td>
                  <span :class="['status-badge', transaction.paymentStatus === 'PAID' ? 'active' : 'pending']">
                    <i :class="[transaction.paymentStatus === 'PAID' ? 'bi bi-check-circle' : 'bi bi-hourglass-split']"></i>
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
                      v-if="isAdmin"
                      @click.stop="openEditModal(transaction)"
                      class="btn-icon edit-btn"
                      title="Sửa"
                    >
                      <i class="bi bi-pencil-square"></i>
                    </button>
                    <button 
                      v-if="isAdmin"
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
        
        <!-- Transaction Summary -->
        <div v-if="transactionsList.length > 0" class="transaction-summary">
          <div class="summary-item">
            <span class="summary-label">Tổng chi phí:</span> 
            <span class="summary-value">{{ formatAmountDisplay(totalAmount) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">Đã thanh toán:</span>
            <span class="summary-value paid">{{ formatAmountDisplay(paidAmount) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">Chưa thanh toán:</span>
            <span class="summary-value pending">{{ formatAmountDisplay(unpaidAmount) }}</span>
          </div>
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
      </div>
    </div>
    
    <!-- Transaction Details Modal -->
    <div v-if="showDetails && selectedTransaction" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">
            <i class="bi bi-info-circle"></i>
            Chi tiết giao dịch
          </h3>
          <button @click="closeDetails" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-header">
            <div class="transaction-avatar">
              <i class="bi bi-cash-coin"></i>
            </div>
            <h3 class="transaction-amount">{{ formatAmountDisplay(safeSelectedTransaction.amount || 0) }}</h3>
            <p class="transaction-date">{{ formatDate(safeSelectedTransaction.transactionDate) }}</p>
          </div>

          <div class="detail-section">
            <h4 class="detail-section-title">Thông tin giao dịch</h4>
          
            <div class="detail-row">
              <div class="detail-label">
                <i class="bi bi-tag"></i>
                Danh mục:
              </div>
              <div class="detail-value">
                {{ safeSelectedTransaction.categoryName }}
              </div>
            </div>
            
            <div class="detail-row">
              <div class="detail-label">
                <i class="bi bi-building"></i>
                Nhà cung cấp:
              </div>
              <div class="detail-value">
                {{ safeSelectedTransaction.supplierName || 'Không có' }}
              </div>
            </div>
            
            <div class="detail-row">
              <div class="detail-label">
                <i class="bi bi-credit-card"></i>
                Trạng thái:
              </div>
              <div class="detail-value">
                <span :class="['status-badge', safeSelectedTransaction.paymentStatus === 'PAID' ? 'active' : 'pending']">
                  <i :class="[safeSelectedTransaction.paymentStatus === 'PAID' ? 'bi bi-check-circle' : 'bi bi-hourglass-split']"></i>
                  {{ formatPaymentStatus(safeSelectedTransaction.paymentStatus) }}
                </span>
              </div>
            </div>
            
            <div class="detail-row">
              <div class="detail-label">
                <i class="bi bi-hash"></i>
                Số tham chiếu:
              </div>
              <div class="detail-value">
                {{ safeSelectedTransaction.referenceNo || 'Không có' }}
              </div>
            </div>
            
            <div class="detail-row">
              <div class="detail-label">
                <i class="bi bi-card-text"></i>
                Mô tả:
              </div>
              <div class="detail-value description-value">
                {{ safeSelectedTransaction.description || 'Không có mô tả' }}
              </div>
            </div>
          </div>
          
          <div v-if="isAdmin" class="modal-actions">
            <button @click="openEditModal(safeSelectedTransaction)" class="btn-primary">
              <i class="bi bi-pencil-square"></i>
              Sửa
            </button>
            <button @click="openDeleteModal(safeSelectedTransaction)" class="btn-danger">
              <i class="bi bi-trash"></i>
              Xóa
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Add/Edit Transaction Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">
            <i :class="editMode ? 'bi bi-pencil-square' : 'bi bi-plus-circle'"></i>
            {{ editMode ? 'Cập nhật giao dịch' : 'Thêm giao dịch mới' }}
          </h3>
          <button @click="closeModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label required-field">Danh mục</label>
              <select 
                v-model="modalTransaction.categoryId" 
                :class="['form-select', modalErrors.categoryId ? 'error' : '']"
                name="categoryId"
              >
                <option value="">Chọn danh mục</option>
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
              <label class="form-label">Nhà cung cấp</label>
              <select v-model="modalTransaction.supplierId" class="form-select">
                <option :value="null">Chọn nhà cung cấp</option>
                <option v-for="supplier in suppliersList" :key="supplier.id" :value="supplier.id">
                  {{ supplier.name }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label required-field">Ngày giao dịch</label>
              <input 
                v-model="modalTransaction.transactionDate" 
                type="date" 
                :class="['form-input', modalErrors.transactionDate ? 'error' : '']"
              />
              <span v-if="modalErrors.transactionDate" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.transactionDate }}
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label required-field">Số tiền</label>
              <input 
                v-model="modalTransaction.amount" 
                type="text" 
                :class="['form-input', modalErrors.amount ? 'error' : '']"
                placeholder="Nhập số tiền"
                @input="validateAmount"
              />
              <span v-if="modalErrors.amount" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.amount }}
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label">Trạng thái thanh toán</label>
              <select v-model="modalTransaction.paymentStatus" class="form-select">
                <option value="PAID">Đã thanh toán</option>
                <option value="UNPAID">Chưa thanh toán</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Số tham chiếu</label>
              <input 
                v-model="modalTransaction.referenceNo" 
                type="text" 
                class="form-input"
                placeholder="Nhập số tham chiếu (nếu có)"
              />
            </div>
            
            <div class="form-group" style="grid-column: span 2;">
              <label class="form-label">Mô tả</label>
              <textarea 
                v-model="modalTransaction.description" 
                class="form-textarea"
                rows="3"
                placeholder="Nhập mô tả giao dịch (nếu có)"
              ></textarea>
            </div>
          </div>
          
          <div class="modal-actions">
            <button @click="closeModal" class="btn-outline" :disabled="saving">
              <i class="bi bi-x-lg"></i>
              Hủy
            </button>
            <button @click="saveTransaction" class="btn-primary" :disabled="saving">
              <i v-if="saving" class="bi bi-arrow-repeat spinner"></i>
              <i v-else class="bi bi-save"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu giao dịch' }}
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

/* Custom tooltip styling for supplier info */
.customer-info {
  position: relative;
  cursor: pointer;
}

.customer-tooltip {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 10;
  background-color: white;
  border: 1px solid #e2e8f0;
  border-radius: 0.375rem;
  padding: 0.75rem;
  min-width: 200px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  font-size: 0.875rem;
}

.customer-info:hover .customer-tooltip {
  display: block;
}

.tooltip-customer-name {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.customer-tooltip div {
  margin-bottom: 0.25rem;
}

.customer-tooltip i {
  margin-right: 0.25rem;
  font-size: 0.75rem;
  color: #4b5563;
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

detail-label i {
  color: #6366f1;
}

.description-value {
  white-space: pre-wrap;
  word-break: break-word;
}

/* Transaction Summary styles */
.transaction-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  background-color: #f8fafc;
  border-radius: 0 0 0.5rem 0.5rem;
}

.summary-item {
  display: flex;
  flex-direction: column;
  min-width: 150px;
}

.summary-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.summary-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
}

.summary-value.paid {
  color: #10b981; /* Green color for paid */
}

.summary-value.pending {
  color: #f59e0b; /* Orange color for pending */
}

@media (max-width: 768px) {
  .transaction-summary {
    flex-direction: column;
    gap: 1rem;
  }
}

/* Add this in your component's <style> section */
.required-field::after {
  content: " *";
  color: red;
}
</style>
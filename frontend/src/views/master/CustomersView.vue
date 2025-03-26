<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { customers } from '../../api/customers';
import { useAuthStore } from '../../stores/auth'; // Thêm store auth để kiểm tra quyền

// Data state
const customersList = ref([]);
const selectedCustomer = ref(null);
const loading = ref(true);
const error = ref(null);
const showDetails = ref(false);

// Auth store để kiểm tra quyền admin
const auth = useAuthStore();

// Kiểm tra quyền admin - Sửa để giống với ExpenseCategoriesView và IncomeCategoriesView
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
const modalCustomer = reactive({
  id: null,
  name: '',
  email: '',
  phone: '',
  address: '',
  taxCode: '', // Thêm thuộc tính taxCode
  notes: '',
  identification: '', // Thêm thuộc tính identification
  isActive: true
});

// Define required fields
const requiredFields = ['name'];

// Change variable name from errors to modalErrors to match what's used in the template
const modalErrors = reactive({
  name: '',
  email: '',
  phone: '',
  identification: '', // Add missing fields
  address: ''
});

// Thêm hàm validate số điện thoại
function validatePhone() {
  // Loại bỏ tất cả ký tự không phải số
  modalCustomer.phone = modalCustomer.phone.replace(/[^0-9]/g, '');
  
  // Giới hạn ở 10 số
  if (modalCustomer.phone.length > 10) {
    modalCustomer.phone = modalCustomer.phone.slice(0, 10);
  }
  
  // Hiển thị lỗi nếu đã nhập nhưng không đủ 10 số
  if (modalCustomer.phone && modalCustomer.phone.length !== 10) {
    modalErrors.phone = 'Số điện thoại phải có đúng 10 chữ số';
  } else {
    modalErrors.phone = '';
  }
}

// Validate email format
function validateEmail() {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (modalCustomer.email && !emailRegex.test(modalCustomer.email)) {
    modalErrors.email = 'Email không hợp lệ';
  } else {
    modalErrors.email = '';
  }
}

// Validate identification
function validateIdentification() {
  // Only allow numeric input - replace any non-numeric characters
  if (modalCustomer.identification) {
    modalCustomer.identification = modalCustomer.identification.replace(/[^0-9]/g, '');
    
    // Limit to 12 digits by only keeping the first 12 digits
    if (modalCustomer.identification.length > 12) {
      modalCustomer.identification = modalCustomer.identification.substring(0, 12);
    }
    
    // Show error if already entered but not complete
    if (modalCustomer.identification && modalCustomer.identification.length !== 12) {
      modalErrors.identification = 'CCCD/CMND phải có đúng 12 chữ số';
    } else {
      modalErrors.identification = '';
    }
  }
}

// States for Delete modal
const showDeleteModal = ref(false);
const customerToDelete = ref(null);
const deleting = ref(false);

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
  }, 3000); // Tự động ẩn sau 3 giây
}

// Open modal to add a new customer
function openAddModal() {
  editMode.value = false;
  modalCustomer.id = null;
  modalCustomer.name = '';
  modalCustomer.email = '';
  modalCustomer.phone = '';
  modalCustomer.address = '';
  modalCustomer.taxCode = ''; // Thêm dòng này
  modalCustomer.notes = '';
  modalCustomer.identification = ''; // Thêm dòng này
  modalCustomer.isActive = true;
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
  modalErrors.identification = '';
  modalErrors.address = '';
  showModal.value = true;
  
  // Focus vào input sau khi modal hiển thị
  nextTick(() => {
    document.querySelector('input[name="customerName"]')?.focus();
  });
}

// Open modal to edit an existing customer
function openEditModal(customer) {
  editMode.value = true;
  modalCustomer.id = customer.id;
  modalCustomer.name = customer.name;
  modalCustomer.email = customer.email;
  modalCustomer.phone = customer.phone || '';
  modalCustomer.address = customer.address || '';
  modalCustomer.taxCode = customer.taxCode || ''; // Thêm dòng này
  modalCustomer.notes = customer.notes || '';
  modalCustomer.identification = customer.identification || ''; // Thêm dòng này
  modalCustomer.isActive = customer.isActive;
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
  modalErrors.identification = '';
  modalErrors.address = '';
  showModal.value = true;
  
  // Focus vào input sau khi modal hiển thị
  nextTick(() => {
    document.querySelector('input[name="customerName"]')?.focus();
  });
}

// Close add/edit modal
function closeModal() {
  showModal.value = false;
}

// Save customer (create or update)
async function saveCustomer() {
  // Reset errors
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
  modalErrors.identification = '';
  modalErrors.address = '';
  
  // Validate
  let isValid = true;
  
  if (!modalCustomer.name || !modalCustomer.name.trim()) {
    modalErrors.name = 'Tên khách hàng không được để trống';
    isValid = false;
  }
  
  if (!modalCustomer.email || !modalCustomer.email.trim()) {
    modalErrors.email = 'Email không được để trống';
    isValid = false;
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(modalCustomer.email)) {
    modalErrors.email = 'Email không hợp lệ';
    isValid = false;
  }
  
  if (!modalCustomer.phone || !modalCustomer.phone.trim()) {
    modalErrors.phone = 'Số điện thoại không được để trống';
    isValid = false;
  } else if (modalCustomer.phone.length !== 10) {
    modalErrors.phone = 'Số điện thoại phải có đúng 10 chữ số';
    isValid = false;
  }
  
  if (!modalCustomer.address || !modalCustomer.address.trim()) {
    modalErrors.address = 'Địa chỉ không được để trống';
    isValid = false;
  }
  
  // Only validate identification if provided
  if (modalCustomer.identification && modalCustomer.identification.length !== 12) {
    modalErrors.identification = 'CCCD/CMND phải có đúng 12 chữ số';
    isValid = false;
  }
  
  if (!isValid) {
    showNotification('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
    return;
  }
  
  saving.value = true;
  try {
    // Prepare the data to match exactly what the backend expects
    const requestData = {
      name: modalCustomer.name.trim(),
      email: modalCustomer.email.trim(),
      phone: modalCustomer.phone.trim(),
      address: modalCustomer.address.trim(),
      identification: modalCustomer.identification ? modalCustomer.identification.trim() : null,
      taxCode: modalCustomer.taxCode ? modalCustomer.taxCode.trim() : null,
      notes: modalCustomer.notes ? modalCustomer.notes.trim() : null,
      isActive: modalCustomer.isActive !== undefined ? modalCustomer.isActive : true
    };
    
    if (editMode.value) {
      await customers.update(modalCustomer.id, requestData);
      showNotification('Cập nhật khách hàng thành công');
      
      // Nếu đang xem chi tiết khách hàng này thì cập nhật lại thông tin chi tiết
      if (selectedCustomer.value && selectedCustomer.value.id === modalCustomer.id) {
        selectedCustomer.value = await customers.getById(modalCustomer.id);
      }
    } else {
      const newCustomer = await customers.create(requestData);
      showNotification('Tạo khách hàng mới thành công');
    }
    
    // Đóng modal trước khi load lại dữ liệu
    closeModal();
    
    // Tải lại từ trang hiện tại
    await loadCustomers(pagination.currentPage);
  } catch (error) {
    console.error('Lỗi khi lưu khách hàng:', error);
    
    if (error.response) {
      const status = error.response.status;
      const message = error.response.data?.message || '';
      
      if (status === 400) {
        if (message.includes('email already exists')) {
          modalErrors.email = 'Email đã tồn tại';
          showNotification('Email đã được sử dụng bởi khách hàng khác', 'error');
        } else if (message.includes('phone already exists')) {
          modalErrors.phone = 'Số điện thoại đã tồn tại';
          showNotification('Số điện thoại đã được sử dụng bởi khách hàng khác', 'error');
        } else if (message.includes('identification already exists')) {
          modalErrors.identification = 'CCCD/CMND đã tồn tại';
          showNotification('CCCD/CMND đã được sử dụng bởi khách hàng khác', 'error');
        } else {
          showNotification(message || 'Dữ liệu không hợp lệ', 'error');
        }
      } else if (status === 500) {
        showNotification('Lỗi máy chủ, vui lòng kiểm tra dữ liệu và thử lại', 'error');
      } else {
        showNotification('Có lỗi xảy ra, vui lòng thử lại sau', 'error');
      }
    } else {
      showNotification('Không thể kết nối đến máy chủ', 'error');
    }
  } finally {
    saving.value = false;
  }
}

// Open delete confirmation modal
function openDeleteModal(customer) {
  customerToDelete.value = customer;
  showDeleteModal.value = true;
}

// Close delete confirmation modal
function closeDeleteModal() {
  showDeleteModal.value = false;
  customerToDelete.value = null;
}

// Delete customer
async function deleteCustomer() {
  if (!customerToDelete.value) return;
  
  const deletedCustomerId = customerToDelete.value.id; // Store ID before closing modal
  
  deleting.value = true;
  try {
    await customers.delete(deletedCustomerId);

    // Đóng modal xóa
    closeDeleteModal();
    
    showNotification('Xóa khách hàng thành công');
  
    
    // Nếu đang xem chi tiết khách hàng này thì đóng chi tiết
    if (selectedCustomer.value && selectedCustomer.value.id === deletedCustomerId) {
      closeDetails();
    }
    
    // Kiểm tra nếu đây là item cuối cùng trên trang hiện tại 
    // và không phải trang đầu tiên
    if (customersList.value.length === 1 && pagination.currentPage > 0) {
      // Nếu đây là item cuối cùng và không phải trang đầu tiên
      // thì chuyển đến trang trước đó
      await loadCustomers(pagination.currentPage - 1);
    } else {
      // Ngược lại, tải lại trang hiện tại
      await loadCustomers(pagination.currentPage);
    }
  } catch (err) {
    console.error('Lỗi khi xóa khách hàng:', err);
    showNotification('Không thể xóa khách hàng', 'error');
  } finally {
    deleting.value = false;
  }
}

// Helper function to get customer details more safely with null checks
function getCustomerName(customerId) {
  if (!customerId) return 'Không có khách hàng';
  const customer = customersList.value.find(c => c.id === customerId);
  return customer ? customer.name : 'Không xác định';
}

// Safe access helper for the selected customer
const safeSelectedCustomer = computed(() => {
  return selectedCustomer.value || {};
});
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
        <div v-if="isAdmin">
          <button @click="openAddModal" class="btn-primary">
            <i class="bi bi-plus-lg"></i>
            Thêm khách hàng
          </button>
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
                  <th class="name-column">Tên khách hàng</th>
                  <th v-if="isAdmin" class="action-column text-center">Hành động</th>
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
                  <td class="customer-name name-column">
                    <i class="bi bi-person customer-icon"></i>
                    {{ customer.name }}
                  </td>
                  <td v-if="isAdmin" class="action-column text-center">
                    <div class="action-buttons">
                      <button @click.stop="openEditModal(customer)" class="btn-icon edit-btn" title="Chỉnh sửa">
                        <i class="bi bi-pencil-square"></i>
                      </button>
                      <button @click.stop="openDeleteModal(customer)" class="btn-icon delete-btn" title="Xóa">
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
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
          
          <div v-else-if="safeSelectedCustomer" class="customer-details-content">
            <div class="detail-header">
              <div class="customer-avatar">
                <i class="bi bi-person-circle"></i>
              </div>
              <h3 class="customer-full-name">{{ safeSelectedCustomer.name || 'Khách hàng' }}</h3>
            </div>
            
            <div class="detail-section">
              <h4 class="detail-section-title">Thông tin liên hệ</h4>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-envelope"></i>
                  Email:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.email || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-telephone"></i>
                  Số điện thoại:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.phone || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-geo-alt"></i>
                  Địa chỉ:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.address || 'Chưa cung cấp' }}</div>
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
                  <span :class="['status-badge', safeSelectedCustomer.isActive ? 'active' : 'inactive']">
                    <i :class="[safeSelectedCustomer.isActive ? 'bi bi-check-circle' : 'bi bi-x-circle']"></i>
                    {{ safeSelectedCustomer.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-card-text"></i>
                  Ghi chú:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.notes || 'Không có ghi chú' }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-receipt"></i>
                  Mã số thuế:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.taxCode || 'Chưa cung cấp' }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-card-text"></i>
                  Số CCCD:
                </div>
                <div class="detail-value">{{ safeSelectedCustomer.identification || 'Chưa cung cấp' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Add/Edit Customer Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">{{ editMode ? 'Chỉnh sửa khách hàng' : 'Thêm khách hàng mới' }}</h3>
          <button @click="closeModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label required">Tên</label>
            <input v-model="modalCustomer.name" name="customerName" type="text" class="form-input" :class="{'error': modalErrors.name}" placeholder="Nhập tên khách hàng">
            <div v-if="modalErrors.name" class="form-error">{{ modalErrors.name }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Email</label>
            <input v-model="modalCustomer.email" type="email" class="form-input" :class="{'error': modalErrors.email}" placeholder="Nhập địa chỉ email" @input="validateEmail" @blur="validateEmail">
            <div v-if="modalErrors.email" class="form-error">
              <i class="bi bi-exclamation-circle"></i>
              {{ modalErrors.email }}
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Số điện thoại</label>
            <input 
              v-model="modalCustomer.phone" 
              type="text" 
              class="form-input" 
              :class="{'error': modalErrors.phone}" 
              placeholder="Nhập số điện thoại (10 chữ số)" 
              @input="validatePhone" 
              maxlength="10"
              inputmode="numeric"
              pattern="[0-9]*"
            >
            <div v-if="modalErrors.phone" class="form-error">
              <i class="bi bi-exclamation-circle"></i>
              {{ modalErrors.phone }}
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">CCCD/CMND</label>
            <input 
              v-model="modalCustomer.identification" 
              type="text" 
              class="form-input" 
              :class="{'error': modalErrors.identification}" 
              placeholder="Nhập số CCCD (12 chữ số)" 
              @input="validateIdentification" 
              maxlength="12"
              inputmode="numeric"
              pattern="[0-9]*"
            >
            <div v-if="modalErrors.identification" class="form-error">
              <i class="bi bi-exclamation-circle"></i>
              {{ modalErrors.identification }}
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Địa chỉ</label>
            <textarea v-model="modalCustomer.address" class="form-textarea" :class="{'error': modalErrors.address}" rows="3" placeholder="Nhập địa chỉ khách hàng"></textarea>
            <div v-if="modalErrors.address" class="form-error">{{ modalErrors.address }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Mã số thuế</label>
            <input v-model="modalCustomer.taxCode" type="text" class="form-input" placeholder="Nhập mã số thuế (nếu có)">
          </div>
          
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <textarea v-model="modalCustomer.notes" class="form-textarea" rows="3" placeholder="Nhập ghi chú (nếu có)"></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">Trạng thái</label>
            <select v-model="modalCustomer.isActive" class="form-input">
              <option :value="true">Hoạt động</option>
              <option :value="false">Không hoạt động</option>
            </select>
          </div>
        </div>
        
        <div class="modal-actions">
          <button @click="closeModal" class="btn-outline">
            <i class="bi bi-x-lg"></i>
            Hủy
          </button>
          <button @click="saveCustomer" class="btn-primary" :disabled="saving">
            <i v-if="saving" class="bi bi-arrow-repeat spinner"></i>
            <i v-else class="bi bi-save"></i>
            {{ saving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Customer Modal -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-container modal-confirm">
        <div class="modal-header">
          <h3 class="modal-title">Xác nhận xóa</h3>
          <button @click="closeDeleteModal" class="btn-close">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-body text-center">
          <div class="icon-warning">
            <i class="bi bi-exclamation-triangle-fill"></i>
          </div>
          <h4 class="confirm-title">Bạn có chắc chắn muốn xóa?</h4>
          <p class="confirm-message">
            Khách hàng "<strong>{{ customerToDelete?.name || 'Không xác định' }}</strong>" sẽ bị xóa vĩnh viễn.
            <br>Hành động này không thể hoàn tác.
          </p>
          
          <div class="modal-actions">
            <button
              @click="closeDeleteModal"
              class="btn-outline"
              :disabled="deleting"
            >
              Hủy
            </button>
            <button
              @click="deleteCustomer"
              class="btn-danger"
              :disabled="deleting"
            >
              <i v-if="deleting" class="bi bi-arrow-repeat spinner"></i>
              {{ deleting ? 'Đang xóa...' : 'Xóa khách hàng' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Toast Notification -->
    <div 
      v-if="notification.show"
      :class="[
        'toast-notification',
        notification.type === 'success' ? 'success' : 'error'
      ]"
    >
      <i :class="
        notification.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
      "></i>
      <span>{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
/* Component-specific styles only */
/* All common styles have been moved to assets/styles/common.css */

/* Add this in your component's <style> section */
.required-field::after {
  content: " *";
  color: red;
}
</style>
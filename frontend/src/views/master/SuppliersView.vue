<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { suppliers } from '../../api/suppliers';
import { useAuthStore } from '../../stores/auth';

// Data state
const suppliersList = ref([]);
const selectedSupplier = ref(null);
const loading = ref(true);
const error = ref(null);
const showDetails = ref(false);

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

// Modal states
const showModal = ref(false);
const editMode = ref(false);
const modalSupplier = reactive({
  id: null,
  name: '',
  email: '',
  phone: '',
  address: '',
  taxCode: '',
  notes: '',
  isActive: true
});
const errors = reactive({
  name: '',
  email: '',
  phone: '',
  address: ''
});

// Add a list of required fields
const requiredFields = ['name'];

const saving = ref(false);

// States for Delete modal
const showDeleteModal = ref(false);
const supplierToDelete = ref(null);
const deleting = ref(false);

// Filter state
const filters = reactive({
  name: '',
  email: '',
  phone: '',
  address: '',
  taxCode: ''
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

// Load suppliers on component mount
onMounted(async () => {
  await loadSuppliers();
});

// Fetch suppliers with pagination and filtering
async function loadSuppliers(newPage = 0) {
  loading.value = true;
  error.value = null;
  
  try {
    // Ensure newPage is a valid number
    const targetPage = parseInt(newPage);
    const page = !isNaN(targetPage) ? targetPage : 0;
    
    const result = await suppliers.getAll(
      filters, 
      page, 
      pagination.pageSize,
      sorting.field,
      sorting.direction
    );
    
    console.log('API pagination response:', result);
    
    // Update data and pagination info
    suppliersList.value = result.content || [];
    
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
    error.value = 'Không thể tải danh sách nhà cung cấp. Vui lòng thử lại sau.';
    console.error('Error loading suppliers:', err);
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
    loadSuppliers(targetPage);
  }
}

// Reset filters
function resetFilters() {
  filters.name = '';
  filters.email = '';
  filters.phone = '';
  filters.address = '';
  filters.taxCode = '';
  pagination.currentPage = 0; // Reset to first page
  loadSuppliers(0);
  closeDetails();
}

// View supplier details
async function showSupplierDetails(supplier) {
  try {
    loading.value = true;
    // If we need to fetch more detailed information
    const detailedSupplier = await suppliers.getById(supplier.id);
    selectedSupplier.value = detailedSupplier;
    showDetails.value = true;
  } catch (err) {
    console.error('Error fetching supplier details:', err);
    error.value = 'Không thể tải thông tin chi tiết nhà cung cấp.';
  } finally {
    loading.value = false;
  }
}

// Close details view
function closeDetails() {
  showDetails.value = false;
  selectedSupplier.value = null;
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
  }, 3000);
}

// Open add modal
function openAddModal() {
  editMode.value = false;
  modalSupplier.id = null;
  modalSupplier.name = '';
  modalSupplier.email = '';
  modalSupplier.phone = '';
  modalSupplier.address = '';
  modalSupplier.taxCode = '';
  modalSupplier.notes = '';
  modalSupplier.isActive = true;
  
  // Clear errors
  Object.keys(errors).forEach(key => errors[key] = '');
  
  showModal.value = true;
  
  // Focus first input
  nextTick(() => {
    document.querySelector('input[name="supplierName"]')?.focus();
  });
}

// Open edit modal
function openEditModal(supplier) {
  editMode.value = true;
  modalSupplier.id = supplier.id;
  modalSupplier.name = supplier.name;
  modalSupplier.email = supplier.email || '';
  modalSupplier.phone = supplier.phone || '';
  modalSupplier.address = supplier.address || '';
  modalSupplier.taxCode = supplier.taxCode || '';
  modalSupplier.notes = supplier.notes || '';
  modalSupplier.isActive = supplier.isActive;
  
  // Clear errors
  errors.name = '';
  errors.email = '';
  errors.phone = '';
  errors.address = '';
  
  showModal.value = true;
  
  // Focus vào input sau khi modal hiển thị
  nextTick(() => {
    document.querySelector('input[name="supplierName"]')?.focus();
  });
}

// Close modal
function closeModal() {
  showModal.value = false;
}

// Open delete confirmation modal
function openDeleteModal(supplier) {
  supplierToDelete.value = supplier;
  showDeleteModal.value = true;
}

// Close delete confirmation modal
function closeDeleteModal() {
  showDeleteModal.value = false;
  supplierToDelete.value = null;
}

// Validate phone number
function validatePhone() {
  // Only allow numeric input - replace any non-numeric characters
  modalSupplier.phone = modalSupplier.phone.replace(/[^0-9]/g, '');
  
  // Limit to 10 digits by only keeping the first 10 digits
  if (modalSupplier.phone.length > 10) {
    modalSupplier.phone = modalSupplier.phone.substring(0, 10);
  }
  
  // Show error if already entered but not complete
  if (modalSupplier.phone && modalSupplier.phone.length !== 10) {
    errors.phone = 'Số điện thoại phải có đúng 10 chữ số';
  } else {
    errors.phone = '';
  }
}

// Validate email format
function validateEmail() {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (modalSupplier.email && !emailRegex.test(modalSupplier.email)) {
    errors.email = 'Email không hợp lệ';
  } else {
    errors.email = '';
  }
}

// Validate form fields
function validateSupplierForm() {
  let isValid = true;
  
  // Clear previous errors
  Object.keys(errors).forEach(key => errors[key] = '');
  
  // Check required fields
  if (!modalSupplier.name || !modalSupplier.name.trim()) {
    errors.name = 'Tên nhà cung cấp không được để trống';
    isValid = false;
  }
  
  if (!modalSupplier.email || !modalSupplier.email.trim()) {
    errors.email = 'Email không được để trống';
    isValid = false;
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(modalSupplier.email)) {
    errors.email = 'Email không hợp lệ';
    isValid = false;
  }
  
  if (!modalSupplier.phone || !modalSupplier.phone.trim()) {
    errors.phone = 'Số điện thoại không được để trống';
    isValid = false;
  } else if (modalSupplier.phone.length !== 10) {
    errors.phone = 'Số điện thoại phải có đúng 10 chữ số';
    isValid = false;
  }
  
  if (!modalSupplier.address || !modalSupplier.address.trim()) {
    errors.address = 'Địa chỉ không được để trống';
    isValid = false;
  }
  
  return isValid;
}

// Save supplier (create or update)
async function saveSupplier() {
  if (!validateSupplierForm()) {
    showNotification('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
    return;
  }
  
  saving.value = true;
  try {
    const requestData = {
      name: modalSupplier.name.trim(),
      email: modalSupplier.email.trim(),
      phone: modalSupplier.phone.trim(),
      address: modalSupplier.address.trim(),
      taxCode: modalSupplier.taxCode ? modalSupplier.taxCode.trim() : null,
      notes: modalSupplier.notes ? modalSupplier.notes.trim() : null,
      isActive: modalSupplier.isActive
    };
    
    if (editMode.value) {
      await suppliers.update(modalSupplier.id, requestData);
      showNotification('Cập nhật nhà cung cấp thành công');
      
      // Nếu đang xem chi tiết nhà cung cấp này thì cập nhật lại thông tin chi tiết
      if (selectedSupplier.value && selectedSupplier.value.id === modalSupplier.id) {
        selectedSupplier.value = await suppliers.getById(modalSupplier.id);
      }
      
      // Đóng modal trước khi load lại dữ liệu
      closeModal();
      
      // Tải lại trang hiện tại
      await loadSuppliers(pagination.currentPage);
    } else {
      const newSupplier = await suppliers.create(requestData);
      showNotification('Tạo nhà cung cấp mới thành công');
      
      // Đóng modal trước khi load lại dữ liệu
      closeModal();
      
      // Cập nhật sorting để hiển thị item mới nhất lên đầu
      sorting.field = 'id';
      sorting.direction = 'desc';
      
      // Reset về trang đầu tiên để thấy item mới nhất
      await loadSuppliers(0);
    }
  } catch (error) {
    console.error('Lỗi khi lưu nhà cung cấp:', error);
    
    if (error instanceof Error) {
      showNotification(error.message, 'error');
    } else if (error.response?.status === 400) {
      if (error.response.data.message?.includes('email already exists')) {
        errors.email = 'Email đã tồn tại';
      } else {
        showNotification(
          error.response.data.message || 'Dữ liệu không hợp lệ',
          'error'
        );
      }
    } else {
      showNotification(
        'Có lỗi xảy ra, vui lòng thử lại sau',
        'error'
      );
    }
  } finally {
    saving.value = false;
  }
}

// Delete supplier
async function deleteSupplier() {
  if (!supplierToDelete.value) return;
  
  const supplierId = supplierToDelete.value.id; // Lưu ID trước khi đóng modal
  const supplierName = supplierToDelete.value.name; // Lưu tên để hiển thị thông báo
  
  deleting.value = true;
  try {
    await suppliers.delete(supplierId);
    
    // Đóng modal xóa trước tiên
    closeDeleteModal();
    
    // Hiển thị thông báo thành công
    showNotification(`Xóa nhà cung cấp "${supplierName}" thành công`);
    
    // Nếu đang xem chi tiết nhà cung cấp này thì đóng chi tiết
    if (selectedSupplier.value && selectedSupplier.value.id === supplierId) {
      closeDetails();
    }
    
    // Kiểm tra nếu đây là item cuối cùng trên trang hiện tại 
    // và không phải trang đầu tiên
    if (suppliersList.value.length === 1 && pagination.currentPage > 0) {
      // Nếu đây là item cuối cùng và không phải trang đầu tiên
      // thì chuyển đến trang trước đó
      await loadSuppliers(pagination.currentPage - 1);
    } else {
      // Ngược lại, tải lại trang hiện tại
      await loadSuppliers(pagination.currentPage);
    }
  } catch (error) {
    console.error('Lỗi khi xóa nhà cung cấp:', error);
    
    if (error instanceof Error) {
      showNotification(error.message, 'error');
    } else if (error.response?.status === 404) {
      // Nếu không tìm thấy (đã bị xóa từ trước)
      showNotification('Nhà cung cấp không tồn tại hoặc đã bị xóa', 'warning');
      // Vẫn cần load lại dữ liệu để đồng bộ với server
      await loadSuppliers(pagination.currentPage);
    } else {
      // Trường hợp lỗi khác
      showNotification('Không thể xóa nhà cung cấp', 'error');
    }
  } finally {
    deleting.value = false;
  }
}

// Sort by field
function sortBy(field) {
  if (sorting.field === field) {
    // Đổi hướng sắp xếp nếu đang sắp xếp theo field này
    sorting.direction = sorting.direction === 'asc' ? 'desc' : 'asc';
  } else {
    // Nếu chọn field mới, mặc định sắp xếp tăng dần
    sorting.field = field;
    sorting.direction = 'asc';
  }
  
  // Load lại dữ liệu với sắp xếp mới
  loadSuppliers(pagination.currentPage);
}

// Safe access helper for the selected supplier
const safeSelectedSupplier = computed(() => {
  return selectedSupplier.value || {};
});
</script>

<template>
  <AppLayout>
    <template #page-title>Quản lý nhà cung cấp</template>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý thông tin nhà cung cấp</h2>
          <p class="page-description">Quản lý danh sách và thông tin chi tiết của các nhà cung cấp</p>
        </div>
        
        <!-- Chỉ hiện nút thêm mới khi là admin -->
        <button 
          v-if="isAdmin"
          @click="openAddModal"
          class="btn-primary"
        >
          <i class="bi bi-plus-lg"></i>
          Thêm nhà cung cấp
        </button>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container" :class="{ 'minimized': showDetails }">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel"></i>
            Tìm kiếm nhà cung cấp
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Tên nhà cung cấp</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-search input-icon-left"></i>
                <input
                  v-model="filters.name"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập tên nhà cung cấp để tìm kiếm"
                />
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Email</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-envelope input-icon-left"></i>
                <input
                  v-model="filters.email"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập email để tìm kiếm"
                />
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Số điện thoại</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-telephone input-icon-left"></i>
                <input
                  v-model="filters.phone"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập số điện thoại để tìm kiếm"
                />
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Địa chỉ</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-geo-alt input-icon-left"></i>
                <input
                  v-model="filters.address"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập địa chỉ để tìm kiếm"
                />
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Mã số thuế</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-receipt input-icon-left"></i>
                <input
                  v-model="filters.taxCode"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập mã số thuế để tìm kiếm"
                />
              </div>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilters" class="btn-outline">
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
            </button>
            <button @click.prevent="loadSuppliers(0)" class="btn-primary">
              <i class="bi bi-search"></i>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
      
      <!-- Supplier List and Details Container -->
      <div class="content-layout" :class="{ 'with-details': showDetails }">
        <!-- Suppliers List -->
        <div class="card suppliers-list" :class="{ 'minimized': showDetails }">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-building"></i>
              Danh sách nhà cung cấp
            </h3>
            <span class="result-count">{{ suppliersList.length }} nhà cung cấp</span>
          </div>
          
          <div v-if="loading && !showDetails" class="card-empty-state">
            <div class="loading-spinner"></div>
            <p>Đang tải dữ liệu...</p>
          </div>
          
          <div v-else-if="error && !showDetails" class="card-empty-state error">
            <i class="bi bi-exclamation-circle error-icon"></i>
            <p>{{ error }}</p>
            <button @click="loadSuppliers" class="btn-primary">
              <i class="bi bi-arrow-repeat"></i>
              Thử lại
            </button>
          </div>
          
          <div v-else-if="suppliersList.length === 0 && !showDetails" class="card-empty-state">
            <i class="bi bi-building-slash empty-icon"></i>
            <p>Không tìm thấy nhà cung cấp nào</p>
            <p class="empty-description">Hãy điều chỉnh bộ lọc tìm kiếm</p>
          </div>
          
          <div v-else class="table-responsive">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="name-column">Tên nhà cung cấp</th>
                  <th class="contact-column">Email</th>
                  <th class="contact-column">Số điện thoại</th>
                  <th class="tax-column">Mã số thuế</th>
                  <th class="address-column">Địa chỉ</th>
                  <th v-if="isAdmin" class="action-column text-center">Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="supplier in suppliersList" 
                  :key="supplier.id" 
                  @click="showSupplierDetails(supplier)"
                  class="supplier-row"
                  :class="{ 'active': selectedSupplier && selectedSupplier.id === supplier.id }"
                >
                  <td class="supplier-name name-column">
                    <i class="bi bi-building supplier-icon"></i>
                    {{ supplier.name }}
                  </td>
                  <td class="contact-column">{{ supplier.email || 'Chưa cung cấp' }}</td>
                  <td class="contact-column">{{ supplier.phone || 'Chưa cung cấp' }}</td>
                  <td class="tax-column">{{ supplier.taxCode || 'Chưa cung cấp' }}</td>
                  <td class="address-column">{{ supplier.address || 'Chưa cung cấp' }}</td>
                  <td v-if="isAdmin" class="action-column text-center">
                    <div class="action-buttons">
                      <button @click.stop="openEditModal(supplier)" class="btn-icon edit-btn" title="Chỉnh sửa">
                        <i class="bi bi-pencil-square"></i>
                      </button>
                      <button @click.stop="openDeleteModal(supplier)" class="btn-icon delete-btn" title="Xóa">
                        <i class="bi bi-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- Pagination Controls -->
          <div v-if="suppliersList.length > 0" class="pagination-container">
            <div class="pagination-info">
              Hiển thị {{ suppliersList.length }} trên tổng số {{ pagination.totalItems }} nhà cung cấp
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
        
        <!-- Supplier Details -->
        <div v-if="showDetails" class="card supplier-details">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-building"></i>
              Chi tiết nhà cung cấp
            </h3>
            <button @click="closeDetails" class="btn-close" title="Đóng chi tiết">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
          
          <div v-if="loading" class="card-empty-state">
            <div class="loading-spinner"></div>
            <p>Đang tải thông tin chi tiết...</p>
          </div>
          
          <div v-else-if="safeSelectedSupplier" class="supplier-details-content">
            <div class="detail-header">
              <div class="supplier-avatar">
                <i class="bi bi-building"></i>
              </div>
              <h3 class="supplier-full-name">{{ safeSelectedSupplier.name || 'Nhà cung cấp' }}</h3>
            </div>
            
            <div class="detail-section">
              <h4 class="detail-section-title">Thông tin liên hệ</h4>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-envelope"></i>
                  Email:
                </div>
                <div class="detail-value">{{ safeSelectedSupplier.email || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-telephone"></i>
                  Số điện thoại:
                </div>
                <div class="detail-value">{{ safeSelectedSupplier.phone || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-receipt"></i>
                  Mã số thuế:
                </div>
                <div class="detail-value">{{ safeSelectedSupplier.taxCode || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-geo-alt"></i>
                  Địa chỉ:
                </div>
                <div class="detail-value">{{ safeSelectedSupplier.address || 'Chưa cung cấp' }}</div>
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
                  <span :class="['status-badge', safeSelectedSupplier.isActive ? 'active' : 'inactive']">
                    <i :class="[safeSelectedSupplier.isActive ? 'bi bi-check-circle' : 'bi bi-x-circle']"></i>
                    {{ safeSelectedSupplier.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-card-text"></i>
                  Ghi chú:
                </div>
                <div class="detail-value">{{ safeSelectedSupplier.notes || 'Không có ghi chú' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Add/Edit Supplier Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">{{ editMode ? 'Chỉnh sửa nhà cung cấp' : 'Thêm nhà cung cấp mới' }}</h3>
          <button @click="closeModal" class="btn-close" title="Đóng">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label required">Tên</label>
            <input v-model="modalSupplier.name" name="supplierName" type="text" class="form-input" :class="{'error': errors.name}" placeholder="Nhập tên nhà cung cấp">
            <div v-if="errors.name" class="form-error">{{ errors.name }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Email</label>
            <input v-model="modalSupplier.email" type="email" class="form-input" :class="{'error': errors.email}" placeholder="Nhập địa chỉ email" @input="validateEmail">
            <div v-if="errors.email" class="form-error">{{ errors.email }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Số điện thoại</label>
            <input 
              v-model="modalSupplier.phone" 
              type="text" 
              class="form-input" 
              :class="{'error': errors.phone}" 
              placeholder="Nhập số điện thoại (10 chữ số)" 
              @input="validatePhone"
              maxlength="10"
              inputmode="numeric"
              pattern="[0-9]*"
            >
            <div v-if="errors.phone" class="form-error">{{ errors.phone }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label required">Địa chỉ</label>
            <textarea v-model="modalSupplier.address" class="form-textarea" :class="{'error': errors.address}" rows="3" placeholder="Nhập địa chỉ nhà cung cấp"></textarea>
            <div v-if="errors.address" class="form-error">{{ errors.address }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Mã số thuế</label>
            <input v-model="modalSupplier.taxCode" type="text" class="form-input" placeholder="Nhập mã số thuế (nếu có)">
          </div>
          
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <textarea v-model="modalSupplier.notes" class="form-textarea" rows="3" placeholder="Nhập ghi chú (nếu có)"></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">Trạng thái</label>
            <select v-model="modalSupplier.isActive" class="form-input">
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
          <button @click="saveSupplier" class="btn-primary" :disabled="saving">
            <i v-if="saving" class="bi bi-arrow-repeat spinner"></i>
            <i v-else class="bi bi-save"></i>
            {{ saving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- Delete Supplier Modal -->
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
            Nhà cung cấp "<strong>{{ supplierToDelete?.name || 'Không xác định' }}</strong>" sẽ bị xóa vĩnh viễn.
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
              @click="deleteSupplier"
              class="btn-danger"
              :disabled="deleting"
            >
              <i v-if="deleting" class="bi bi-arrow-repeat spinner"></i>
              {{ deleting ? 'Đang xóa...' : 'Xóa nhà cung cấp' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Notification -->
    <div v-if="notification.show" :class="['toast-notification', notification.type]">
      <i :class="notification.type === 'success' ? 'bi bi-check-circle' : 'bi bi-exclamation-circle'"></i>
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

/* Styles for the contact information columns */
.contact-column {
  min-width: 150px;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tax-column {
  min-width: 120px;
  max-width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.address-column {
  min-width: 150px;
  max-width: 250px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Make table responsive */
.table-responsive {
  overflow-x: auto;
}

@media (max-width: 768px) {
  .contact-column, .address-column, .tax-column {
    display: none;
  }
}
</style>
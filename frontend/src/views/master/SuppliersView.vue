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
  contactPerson: '',
  notes: '',
  isActive: true
});
const modalErrors = reactive({
  name: '',
  email: '',
  phone: ''
});
const saving = ref(false);

// States for Delete modal
const showDeleteModal = ref(false);
const supplierToDelete = ref(null);
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
  modalSupplier.contactPerson = '';
  modalSupplier.notes = '';
  modalSupplier.isActive = true;
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
  showModal.value = true;
  
  // Focus vào input sau khi modal hiển thị
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
  modalSupplier.contactPerson = supplier.contactPerson || '';
  modalSupplier.notes = supplier.notes || '';
  modalSupplier.isActive = supplier.isActive;
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
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
  // Loại bỏ tất cả ký tự không phải số
  modalSupplier.phone = modalSupplier.phone.replace(/\D/g, '');
  
  // Giới hạn ở 10 số
  if (modalSupplier.phone.length > 10) {
    modalSupplier.phone = modalSupplier.phone.slice(0, 10);
  }
  
  // Hiển thị lỗi nếu đã nhập nhưng không đủ 10 số
  if (modalSupplier.phone && modalSupplier.phone.length < 10) {
    modalErrors.phone = 'Số điện thoại phải có đúng 10 chữ số';
  } else {
    modalErrors.phone = '';
  }
}

// Save supplier (create or update)
async function saveSupplier() {
  // Reset errors
  modalErrors.name = '';
  modalErrors.email = '';
  modalErrors.phone = '';
  
  // Validate
  let isValid = true;
  
  if (!modalSupplier.name.trim()) {
    modalErrors.name = 'Tên nhà cung cấp không được để trống';
    isValid = false;
  }
  
  if (modalSupplier.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(modalSupplier.email)) {
    modalErrors.email = 'Email không hợp lệ';
    isValid = false;
  }
  
  // Kiểm tra nếu số điện thoại đã nhập thì phải đúng 10 số
  if (modalSupplier.phone && modalSupplier.phone.length !== 10) {
    modalErrors.phone = 'Số điện thoại phải có đúng 10 chữ số';
    isValid = false;
  }
  
  if (!isValid) return;
  
  saving.value = true;
  try {
    if (editMode.value) {
      await suppliers.update(modalSupplier.id, {
        name: modalSupplier.name,
        email: modalSupplier.email,
        phone: modalSupplier.phone,
        address: modalSupplier.address,
        taxCode: modalSupplier.taxCode,
        contactPerson: modalSupplier.contactPerson,
        notes: modalSupplier.notes,
        isActive: modalSupplier.isActive
      });
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
      const newSupplier = await suppliers.create({
        name: modalSupplier.name,
        email: modalSupplier.email,
        phone: modalSupplier.phone,
        address: modalSupplier.address,
        taxCode: modalSupplier.taxCode,
        contactPerson: modalSupplier.contactPerson,
        notes: modalSupplier.notes,
        isActive: modalSupplier.isActive
      });
      showNotification('Tạo nhà cung cấp mới thành công');
      
      // Đóng modal trước khi load lại dữ liệu
      closeModal();
      
      // Tải lại từ trang hiện tại thay vì về trang đầu tiên
      await loadSuppliers(pagination.currentPage);
    }
  } catch (error) {
    console.error('Lỗi khi lưu nhà cung cấp:', error);
    
    if (error instanceof Error) {
      showNotification(error.message, 'error');
    } else if (error.response?.status === 400) {
      if (error.response.data.message?.includes('email already exists')) {
        modalErrors.email = 'Email đã tồn tại';
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
                  <td v-if="isAdmin" class="action-column text-center">
                    <div class="action-buttons">
                      <button @click.stop="openEditModal(supplier)" class="btn-outline btn-icon">
                        <i class="bi bi-pencil"></i>
                      </button>
                      <button @click.stop="openDeleteModal(supplier)" class="btn-outline btn-icon">
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
              <i class="bi bi-building-add"></i>
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
          
          <div v-else-if="selectedSupplier" class="supplier-details-content">
            <div class="detail-header">
              <div class="supplier-avatar">
                <i class="bi bi-building"></i>
              </div>
              <h3 class="supplier-full-name">{{ selectedSupplier.name }}</h3>
            </div>
            
            <div class="detail-section">
              <h4 class="detail-section-title">Thông tin liên hệ</h4>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-envelope"></i>
                  Email:
                </div>
                <div class="detail-value">{{ selectedSupplier.email || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-telephone"></i>
                  Số điện thoại:
                </div>
                <div class="detail-value">{{ selectedSupplier.phone || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-receipt"></i>
                  Mã số thuế:
                </div>
                <div class="detail-value">{{ selectedSupplier.taxCode || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-person"></i>
                  Người liên hệ:
                </div>
                <div class="detail-value">{{ selectedSupplier.contactPerson || 'Chưa cung cấp' }}</div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-geo-alt"></i>
                  Địa chỉ:
                </div>
                <div class="detail-value">{{ selectedSupplier.address || 'Chưa cung cấp' }}</div>
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
                  <span :class="['status-badge', selectedSupplier.isActive ? 'active' : 'inactive']">
                    <i :class="[selectedSupplier.isActive ? 'bi bi-check-circle' : 'bi bi-x-circle']"></i>
                    {{ selectedSupplier.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </div>
              </div>
              
              <div class="detail-row">
                <div class="detail-label">
                  <i class="bi bi-card-text"></i>
                  Ghi chú:
                </div>
                <div class="detail-value">{{ selectedSupplier.notes || 'Không có ghi chú' }}</div>
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
            <label class="form-label">Tên nhà cung cấp</label>
            <input
              v-model="modalSupplier.name"
              type="text"
              class="form-input"
              name="supplierName"
              :class="{ 'is-invalid': modalErrors.name }"
              placeholder="Nhập tên nhà cung cấp"
            />
            <div v-if="modalErrors.name" class="invalid-feedback">{{ modalErrors.name }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              v-model="modalSupplier.email"
              type="email"
              class="form-input"
              :class="{ 'is-invalid': modalErrors.email }"
              placeholder="Nhập email"
            />
            <div v-if="modalErrors.email" class="invalid-feedback">{{ modalErrors.email }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Số điện thoại</label>
            <input
              v-model="modalSupplier.phone"
              type="tel"
              class="form-input"
              :class="{ 'is-invalid': modalErrors.phone }"
              placeholder="Nhập số điện thoại nhà cung cấp"
              maxlength="10"
              @input="validatePhone"
            />
            <div v-if="modalErrors.phone" class="invalid-feedback">{{ modalErrors.phone }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Mã số thuế</label>
            <input
              v-model="modalSupplier.taxCode"
              type="text"
              class="form-input"
              placeholder="Nhập mã số thuế (nếu có)"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">Người liên hệ</label>
            <input
              v-model="modalSupplier.contactPerson"
              type="text"
              class="form-input"
              placeholder="Nhập tên người liên hệ"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">Địa chỉ</label>
            <textarea
              v-model="modalSupplier.address"
              class="form-input"
              placeholder="Nhập địa chỉ nhà cung cấp"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <textarea
              v-model="modalSupplier.notes"
              class="form-input"
              placeholder="Nhập ghi chú (nếu có)"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">Trạng thái</label>
            <div class="form-switch">
              <input
                v-model="modalSupplier.isActive"
                type="checkbox"
                class="form-switch-input"
                id="supplierActive"
              />
              <label class="form-switch-label" for="supplierActive">
                {{ modalSupplier.isActive ? 'Hoạt động' : 'Không hoạt động' }}
              </label>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeModal" class="btn-outline">
            <i class="bi bi-x-lg"></i>
            Hủy bỏ
          </button>
          <button @click="saveSupplier" :disabled="saving" class="btn-primary">
            <i v-if="saving" class="bi bi-arrow-repeat loading-icon"></i>
            <i v-else class="bi bi-save"></i>
            Lưu
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
            Nhà cung cấp "<strong>{{ supplierToDelete?.name }}</strong>" sẽ bị xóa vĩnh viễn.
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
    <div v-if="notification.show" :class="['notification', notification.type]">
      <i :class="['notification-icon', notification.type === 'success' ? 'bi bi-check-circle' : 'bi bi-exclamation-circle']"></i>
      <span class="notification-message">{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-container {
  padding: 2rem;
  background-color: #f9fafb;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

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
  margin-top: 0.5rem;
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
  background-color: transparent;
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

.btn-close {
  background-color: transparent;
  color: #6b7280;
  border: none;
  cursor: pointer;
}

.btn-close i {
  font-size: 1.25rem;
}

.filter-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  padding: 1rem;
}

.filter-container.minimized {
  display: none;
}

.filter-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.filter-header .card-title {
  color: #111827;
  margin: 0;
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
  grid-template-columns: 1fr;
  gap: 1rem;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.filter-item .form-label {
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.filter-item .form-input {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}

.filter-item .input-icon-wrapper {
  position: relative;
}

.filter-item .input-icon-wrapper .input-icon-left {
  position: absolute;
  left: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  color: #6b7280;
}

.filter-item .form-input.has-icon {
  padding-left: 2rem;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.content-layout {
  display: flex;
  gap: 2rem;
}

.content-layout.with-details .suppliers-list {
  flex: 1;
}

.content-layout.with-details .supplier-details {
  flex: 1;
}

.card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1rem;
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
  display: flex;
  align-items: center;
}

.card-title i {
  margin-right: 0.5rem;
}

.result-count {
  color: #6b7280;
}

.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
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
  margin-bottom: 1rem;
}

.card-empty-state .error-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.card-empty-state .empty-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.card-empty-state .empty-description {
  color: #6b7280;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #d1d5db;
}

.data-table th {
  background-color: #f3f4f6;
  color: #111827;
}

.data-table td {
  color: #6b7280;
}

.name-column {
  width: 70%;
}

.action-column {
  width: 30%;
}

.supplier-row {
  cursor: pointer;
}

.supplier-row.active {
  background-color: #e5e7eb;
}

.supplier-icon {
  margin-right: 0.5rem;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

.btn-icon {
  background-color: transparent;
  color: #6b7280;
  border: none;
  cursor: pointer;
}

.btn-icon i {
  font-size: 1.25rem;
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
  gap: 0.5rem;
}

.pagination-btn {
  background-color: transparent;
  color: #6b7280;
  border: none;
  cursor: pointer;
}

.pagination-btn:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

.pagination-text {
  color: #6b7280;
}

.supplier-details {
  flex: 1;
}

.supplier-details-content {
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.supplier-avatar {
  width: 3rem;
  height: 3rem;
  background-color: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
}

.supplier-full-name {
  color: #111827;
  margin: 0;
}

.detail-section {
  margin-bottom: 1rem;
}

.detail-section-title {
  color: #111827;
  margin-bottom: 0.5rem;
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
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.status-badge.active {
  color: #10b981;
}

.status-badge.inactive {
  color: #ef4444;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  width: 90%;
  max-width: 500px;
  padding: 1rem;
}

.modal-container.modal-confirm {
  max-width: 26rem;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.modal-title {
  color: #111827;
  margin: 0;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-height: 70vh; /* Giới hạn chiều cao tối đa */
  overflow-y: auto; /* Thêm thanh cuộn dọc khi nội dung vượt quá */
}

/* Thêm style để sửa lỗi khi cuộn với Firefox */
.modal-body {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e0 #f8fafc;
}

/* Tùy chỉnh thanh cuộn cho Chrome, Edge và Safari */
.modal-body::-webkit-scrollbar {
  width: 8px;
}

.modal-body::-webkit-scrollbar-track {
  background: #f8fafc;
  border-radius: 4px;
}

.modal-body::-webkit-scrollbar-thumb {
  background-color: #cbd5e0;
  border-radius: 4px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background-color: #a0aec0;
}

.modal-body.text-center {
  text-align: center;
}

.icon-warning {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.icon-warning i {
  font-size: 2rem;
  color: #f59e0b;
}

.confirm-title {
  font-weight: 600;
  font-size: 1.125rem;
  margin-bottom: 0.5rem;
  color: #1e293b;
  text-align: center;
  width: 100%;
}

.confirm-message {
  color: #4b5563;
  text-align: center;
  width: 100%;
  margin-bottom: 1.5rem;
}

.modal-body.text-center .modal-actions {
  display: flex;
  justify-content: center;
  width: 100%;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.spinner {
  animation: spin 1s linear infinite;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.form-input {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}

.form-input.is-invalid {
  border-color: #ef4444;
}

.invalid-feedback {
  color: #ef4444;
  margin-top: 0.25rem;
}

.form-switch {
  display: flex;
  align-items: center;
}

.form-switch-input {
  margin-right: 0.5rem;
}

.form-switch-label {
  color: #6b7280;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.notification {
  position: fixed;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  z-index: 1000;
}

.notification.success {
  border-left: 4px solid #10b981;
}

.notification.error {
  border-left: 4px solid #ef4444;
}

.notification.warning {
  border-left: 4px solid #f59e0b;
}

.notification-icon {
  font-size: 1.5rem;
}

.notification-message {
  color: #111827;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
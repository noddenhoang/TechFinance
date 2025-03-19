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
            <label class="form-label">Địa chỉ</label>
            <textarea
              v-model="modalSupplier.address"
              class="form-input"
              placeholder="Nhập địa chỉ nhà cung cấp"
              rows="3"
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
            <select v-model="modalSupplier.isActive" class="form-input">
              <option :value="true">Hoạt động</option>
              <option :value="false">Không hoạt động</option>
            </select>
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

.supplier-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.supplier-row:hover {
  background-color: #f1f5f9;
}

.supplier-row.active {
  background-color: #eff6ff;
}

.supplier-name {
  font-weight: 500;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.supplier-icon {
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

/* SUPPLIER DETAILS STYLES */
.content-layout {
  display: flex;
  flex-direction: column;
}

.content-layout.with-details {
  flex-direction: row;
  gap: 1.5rem;
}

.suppliers-list {
  flex: 1;
}

.supplier-details {
  width: 100%;
  max-width: 30rem;
}

.supplier-details-content {
  padding: 1.5rem;
}

.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1.5rem;
}

.supplier-avatar {
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  background-color: #e0e7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.75rem;
}

.supplier-avatar i {
  font-size: 2.5rem;
  color: #4f46e5;
}

.supplier-full-name {
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

/* MODAL STYLES */
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
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  width: 100%;
  max-width: 30rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.modal-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8fafc;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
}

.modal-body {
  padding: 1.5rem;
  max-height: 70vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e0 #f8fafc;
}

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

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.invalid-feedback {
  color: #b91c1c;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.is-invalid {
  border-color: #b91c1c;
}

/* Responsive styles */
@media (max-width: 768px) {
  .content-layout.with-details {
    flex-direction: column;
  }
  
  .suppliers-list.minimized {
    display: none;
  }
  
  .supplier-details {
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

/* Thêm style mới cho các cột */
.name-column {
  text-align: left;
}

.action-column {
  width: 140px; /* Đặt chiều rộng cố định */
}

.text-center {
  text-align: center !important;
}

.action-buttons {
  display: flex;
  justify-content: center; /* Căn giữa các nút */
  gap: 0.5rem;
}

.btn-icon {
  padding: 0.5rem;
  min-width: 36px;
  height: 36px;
}

/* Toast Notification Styles */
.notification {
  position: fixed;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background-color: white;
  border-radius: 0.375rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  z-index: 1000;
  animation: fade-in 0.3s ease-out;
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
  font-size: 1.25rem;
}

.notification.success .notification-icon {
  color: #10b981;
}

.notification.error .notification-icon {
  color: #ef4444;
}

.notification.warning .notification-icon {
  color: #f59e0b;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translate(-50%, 20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

/* Cập nhật các style của modal delete để đồng bộ */
.modal-confirm {
  max-width: 26rem;
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
  justify-content: flex-end !important;
  width: 100%;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

/* Cập nhật style cho nút Xóa nhà cung cấp */
.btn-danger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-danger:hover {
  background-color: #dc2626;
}

.spinner {
  animation: spin 1s linear infinite;
}
</style>
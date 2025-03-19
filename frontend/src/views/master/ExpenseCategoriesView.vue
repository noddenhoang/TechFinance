<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import AppLayout from '../../components/layouts/AppLayout.vue'
import { expenseCategories } from '../../api/expenseCategories'
import { useAuthStore } from '../../stores/auth'

// Store và xác thực
const auth = useAuthStore()

// Kiểm tra quyền admin
const isAdmin = computed(() => {
  // Debug dữ liệu auth nếu cần
  console.log('Auth user data:', auth.user)
  console.log('Auth authorities:', auth.user?.authorities)
  
  // Kiểm tra theo nhiều cấu trúc có thể có
  if (auth.user?.authorities && Array.isArray(auth.user.authorities)) {
    // Trường hợp 1: authorities là mảng string
    if (typeof auth.user.authorities[0] === 'string') {
      return auth.user.authorities.includes('ROLE_ADMIN')
    }
    
    // Trường hợp 2: authorities là mảng object có thuộc tính authority
    if (typeof auth.user.authorities[0] === 'object') {
      return auth.user.authorities.some(auth => 
        auth.authority === 'ROLE_ADMIN' || auth === 'ROLE_ADMIN'
      )
    }
  }
  
  // Trường hợp 3: role trực tiếp trên user
  if (auth.user?.role === 'ROLE_ADMIN' || auth.user?.role === 'admin') {
    return true
  }
  
  // Không tìm thấy quyền admin
  return false
})

// Data state
const categories = ref([])
const loading = ref(true)
const error = ref(null)

// Filter state
const filters = reactive({
  name: '',
  isActive: null
})

// Modal states
const showModal = ref(false)
const editMode = ref(false)
const modalCategory = reactive({
  id: null,
  name: '',
  description: '',
  isActive: true
})
const modalErrors = reactive({
  name: ''
})

// Delete modal state
const showDeleteModal = ref(false)
const categoryToDelete = ref(null)
const deleting = ref(false)

// Operation states
const saving = ref(false)

// Toast notification
const notification = reactive({
  show: false,
  message: '',
  type: 'success'
})

// Thêm state cho phân trang
const pagination = reactive({
  currentPage: 0,
  totalPages: 0,
  totalItems: 0,
  pageSize: 8
})

// Thêm state cho sắp xếp
const sorting = reactive({
  field: 'id',
  direction: 'asc'
})

// Load categories on component mount
onMounted(async () => {
  await loadCategories()
})

// Sửa hàm loadCategories để hỗ trợ phân trang
async function loadCategories(newPage = 0) {
  loading.value = true;
  error.value = null;
  try {
    // Đảm bảo newPage là số hợp lệ
    const targetPage = parseInt(newPage);
    const page = !isNaN(targetPage) ? targetPage : 0;
    
    const result = await expenseCategories.getAll(
      filters, 
      page, 
      pagination.pageSize,
      sorting.field,
      sorting.direction
    );
    
    console.log('API pagination response:', result);
    
    // Cập nhật dữ liệu và thông tin phân trang
    categories.value = result.content || [];
    
    // Cập nhật thông tin phân trang từ API response
    if (result.page) {
      pagination.currentPage = result.page.page;
      pagination.totalPages = result.page.totalPages;
      pagination.totalItems = result.page.totalElement;
    } else {
      pagination.currentPage = page;
      pagination.totalPages = Math.ceil((result.totalElement || 0) / pagination.pageSize);
      pagination.totalItems = result.totalElement || 0;
    }
    
  } catch (err) {
    error.value = 'Không thể tải danh mục. Vui lòng thử lại sau.';
    console.error('Error loading categories:', err);
  } finally {
    loading.value = false;
  }
}

// Thêm hàm chuyển trang
function goToPage(page) {
  // Đảm bảo page là số
  const targetPage = parseInt(page);
  
  // Kiểm tra nếu là số hợp lệ và trong khoảng cho phép
  if (!isNaN(targetPage) && targetPage >= 0 && targetPage < pagination.totalPages) {
    loadCategories(targetPage);
  }
}

// Thêm hàm này sau hàm goToPage
function sortBy(field) {
  if (sorting.field === field) {
    sorting.direction = sorting.direction === 'asc' ? 'desc' : 'asc';
  } else {
    sorting.field = field;
    sorting.direction = 'asc';
  }
  loadCategories(0); // Reset về trang đầu tiên khi thay đổi sắp xếp
}

function resetFilters() {
  filters.name = '';
  filters.isActive = null;
  pagination.currentPage = 0; // Reset về trang đầu tiên
  loadCategories(0);
}

function openAddModal() {
  editMode.value = false
  modalCategory.id = null
  modalCategory.name = ''
  modalCategory.description = ''
  modalCategory.isActive = true
  modalErrors.name = ''
  showModal.value = true
  
  // Focus vào input sau khi modal hiển thị
  nextTick(() => {
    document.querySelector('input[type="text"]')?.focus()
  })
}

function openEditModal(category) {
  editMode.value = true
  modalCategory.id = category.id
  modalCategory.name = category.name
  modalCategory.description = category.description || ''
  modalCategory.isActive = category.isActive
  modalErrors.name = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveCategory() {
  // Reset errors
  modalErrors.name = ''
  
  // Validate
  if (!modalCategory.name.trim()) {
    modalErrors.name = 'Tên danh mục không được để trống'
    return
  }
  
  saving.value = true
  try {
    if (editMode.value) {
      await expenseCategories.update(modalCategory.id, {
        name: modalCategory.name,
        description: modalCategory.description,
        isActive: modalCategory.isActive
      })
      showNotification('Cập nhật danh mục thành công')
    } else {
      await expenseCategories.create({
        name: modalCategory.name,
        description: modalCategory.description,
        isActive: modalCategory.isActive
      })
      showNotification('Tạo danh mục mới thành công')
    }
    closeModal()
    await loadCategories()
  } catch (err) {
    if (err.response?.data?.message?.includes('already exists')) {
      modalErrors.name = 'Danh mục với tên này đã tồn tại'
    } else {
      showNotification(err.response?.data?.message || 'Đã xảy ra lỗi khi lưu danh mục', 'error')
    }
  } finally {
    saving.value = false
  }
}

function confirmDelete(category) {
  categoryToDelete.value = category
  showDeleteModal.value = true
}

async function deleteCategory() {
  if (!categoryToDelete.value) return
  
  deleting.value = true
  try {
    await expenseCategories.delete(categoryToDelete.value.id)
    showNotification('Xóa danh mục thành công')
    closeDeleteModal()
    await loadCategories()
  } catch (err) {
    showNotification('Không thể xóa danh mục', 'error')
  } finally {
    deleting.value = false
  }
}

function closeDeleteModal() {
  showDeleteModal.value = false
  categoryToDelete.value = null
}

function showNotification(message, type = 'success') {
  notification.message = message
  notification.type = type
  notification.show = true
  
  setTimeout(() => {
    notification.show = false
  }, 3000)
}

const statusOptions = [
  { value: null, label: 'Tất cả' },
  { value: true, label: 'Hoạt động' },
  { value: false, label: 'Không hoạt động' }
]
</script>

<template>
  <AppLayout>
    <template #page-title>Danh mục chi phí</template>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý danh mục chi phí</h2>
          <p class="page-description">Quản lý các danh mục phân loại chi phí của doanh nghiệp</p>
        </div>
        
        <!-- Chỉ hiện nút thêm mới khi là admin -->
        <button 
          v-if="isAdmin"
          @click="openAddModal"
          class="btn-primary"
        >
          <i class="bi bi-plus-circle"></i> Thêm mới
        </button>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel"></i>
            Tìm kiếm danh mục
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Tên danh mục</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-search input-icon-left"></i>
                <input
                  v-model="filters.name"
                  type="text"
                  class="form-input has-icon"
                  placeholder="Nhập tên để tìm kiếm"
                />
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Trạng thái</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-toggle-on input-icon-left"></i>
                <select v-model="filters.isActive" class="form-select has-icon">
                  <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilters" class="btn-outline">
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
            </button>
            <button @click.prevent="loadCategories(0)" class="btn-primary">
              <i class="bi bi-search"></i>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
      
      <!-- Categories Table -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="bi bi-list-ul"></i>
            Danh sách danh mục
          </h3>
          <span class="result-count">{{ categories.length }} danh mục</span>
        </div>
        
        <div v-if="loading" class="card-empty-state">
          <div class="loading-spinner"></div>
          <p>Đang tải dữ liệu...</p>
        </div>
        
        <div v-else-if="error" class="card-empty-state error">
          <i class="bi bi-exclamation-circle error-icon"></i>
          <p>{{ error }}</p>
          <button @click="loadCategories" class="btn-primary">
            <i class="bi bi-arrow-repeat"></i>
            Thử lại
          </button>
        </div>
        
        <div v-else-if="categories.length === 0" class="card-empty-state">
          <i class="bi bi-folder2-open empty-icon"></i>
          <p>Không tìm thấy danh mục chi phí nào</p>
          <p class="empty-description">Hãy thêm danh mục mới hoặc điều chỉnh bộ lọc</p>
        </div>
        
        <div v-else class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th class="sortable" @click="sortBy('name')">
                  Tên danh mục
                  <i v-if="sorting.field === 'name'" 
                     :class="['sort-icon', 'bi', sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down']"></i>
                </th>
                <th class="sortable" @click="sortBy('description')">
                  Mô tả
                  <i v-if="sorting.field === 'description'" 
                     :class="['sort-icon', 'bi', sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down']"></i>
                </th>
                <th class="sortable" @click="sortBy('isActive')">
                  Trạng thái
                  <i v-if="sorting.field === 'isActive'" 
                     :class="['sort-icon', 'bi', sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down']"></i>
                </th>
                <th v-if="isAdmin" class="text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="category in categories" :key="category.id">
                <td class="font-medium">{{ category.name }}</td>
                <td class="description-cell">{{ category.description || '–' }}</td>
                <td>
                  <span
                    class="status-badge"
                    :class="[category.isActive ? 'active' : 'inactive']"
                  >
                    <i :class="[
                      category.isActive ? 'bi bi-check-circle' : 'bi bi-x-circle'
                    ]"></i>
                    {{ category.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </td>
                <td v-if="isAdmin" class="actions-cell">
                  <div class="action-buttons">
                    <button 
                      @click="openEditModal(category)"
                      class="btn-icon edit-btn"
                      title="Chỉnh sửa"
                    >
                      <i class="bi bi-pencil-square"></i>
                    </button>
                    <button 
                      @click="confirmDelete(category)"
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
      </div>
      
      <!-- Thêm điều khiển phân trang -->
      <div v-if="categories.length > 0" class="pagination-container">
        <div class="pagination-info">
          Hiển thị {{ categories.length }} trên tổng số {{ pagination.totalItems }} danh mục
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
    
    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ editMode ? 'Chỉnh sửa danh mục' : 'Thêm danh mục mới' }}
          </h3>
          <button @click="closeModal" class="btn-close">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <form @submit.prevent="saveCategory">
            <div class="form-group">
              <label class="form-label required">
                Tên danh mục
              </label>
              <input
                v-model="modalCategory.name"
                type="text"
                class="form-input"
                :class="{ 'error': modalErrors.name }"
                placeholder="Nhập tên danh mục"
                required
              />
              <p v-if="modalErrors.name" class="form-error">
                <i class="bi bi-exclamation-circle"></i>
                {{ modalErrors.name }}
              </p>
            </div>
            
            <div class="form-group">
              <label class="form-label">
                Mô tả
              </label>
              <textarea
                v-model="modalCategory.description"
                rows="3"
                class="form-textarea"
                placeholder="Nhập mô tả cho danh mục này"
              ></textarea>
            </div>
            
            <div class="form-group">
              <div class="checkbox-wrapper">
                <input
                  v-model="modalCategory.isActive"
                  type="checkbox"
                  id="isActive"
                  class="checkbox-input"
                />
                <label for="isActive" class="checkbox-label">
                  Kích hoạt danh mục này
                </label>
              </div>
              <p class="help-text">Chỉ các danh mục đang hoạt động mới hiển thị trong các tùy chọn lựa chọn.</p>
            </div>
            
            <div class="modal-actions">
              <button
                type="button"
                @click="closeModal"
                class="btn-outline"
                :disabled="saving"
              >
                Hủy
              </button>
              <button
                type="submit"
                class="btn-primary"
                :disabled="saving"
              >
                <i v-if="saving" class="bi bi-arrow-repeat spinner"></i>
                {{ saving ? 'Đang lưu...' : (editMode ? 'Cập nhật' : 'Tạo mới') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- Delete Confirmation Modal -->
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
            Danh mục "<strong>{{ categoryToDelete?.name }}</strong>" sẽ bị xóa vĩnh viễn.
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
              @click="deleteCategory"
              class="btn-danger"
              :disabled="deleting"
            >
              <i v-if="deleting" class="bi bi-arrow-repeat spinner"></i>
              {{ deleting ? 'Đang xóa...' : 'Xóa danh mục' }}
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
      <i :class="[
        notification.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
      ]"></i>
      <span>{{ notification.message }}</span>
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

@media (min-width: 768px) {
  .filter-grid {
    grid-template-columns: repeat(3, 1fr);
  }
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
.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.form-label.required::after {
  content: '*';
  color: #ef4444;
  margin-left: 0.25rem;
}

.form-input, 
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: white;
  font-size: 0.875rem;
  color: #1f2937;
  transition: all 0.2s ease;
}

.form-input:focus, 
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.2);
}

.form-input.has-icon, 
.form-select.has-icon {
  padding-left: 2.5rem;
}

.form-input.error, 
.form-select.error {
  border-color: #ef4444;
}

.form-input.error:focus, 
.form-select.error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

.form-error {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: #ef4444;
  font-size: 0.75rem;
  margin-top: 0.5rem;
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

.help-text {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.375rem;
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

.data-table tr:hover {
  background-color: #f8fafc;
}

.font-medium {
  font-weight: 500;
  color: #111827;
}

.description-cell {
  max-width: 16rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #6b7280;
}

.text-right {
  text-align: right;
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

/* ACTION BUTTONS */
.actions-cell {
  text-align: right;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.375rem;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
}

.edit-btn {
  background-color: #e0e7ff;
  color: #4f46e5;
}

.edit-btn:hover {
  background-color: #c7d2fe;
  transform: translateY(-2px);
}

.delete-btn {
  background-color: #fee2e2;
  color: #ef4444;
}

.delete-btn:hover {
  background-color: #fecaca;
  transform: translateY(-2px);
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

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
  max-width: 32rem;
  overflow: hidden;
}

.modal-confirm {
  max-width: 26rem;
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
}

.modal-actions {
  display: flex;
  justify-content: flex-end; /* Căn phải */
  width: 100%;
  gap: 0.75rem;
  margin-top: 1.5rem;
  border-top: none;
  padding-top: 0;
}

.modal-body.text-center .modal-actions {
  display: flex;
  justify-content: flex-end !important;
  width: 100%;
  gap: 0.75rem;
  margin-top: 1.5rem;
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

/* TOAST NOTIFICATION */
.toast-notification {
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
}

.toast-notification.success {
  border-left: 4px solid #10b981;
}

.toast-notification.error {
  border-left: 4px solid #ef4444;
}

.toast-notification i {
  font-size: 1.25rem;
}

/* Thêm CSS cho phân trang */
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

/* Thêm định nghĩa cho .text-center */
.text-center {
  text-align: center !important;
}
</style>
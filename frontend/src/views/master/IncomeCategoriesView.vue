<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import AppLayout from '../../components/layouts/AppLayout.vue'
import { incomeCategories } from '../../api/incomeCategories'
import { useAuthStore } from '../../stores/auth'

// Store và xác thực
const auth = useAuthStore()
// Debug auth store để xem cấu trúc dữ liệu
console.log('Auth store content:', auth)

// Tạm thời cho phép tất cả người dùng có quyền admin để kiểm tra chức năng UI
// Sau khi xác định đúng cấu trúc quyền, sẽ thay thế bằng cách kiểm tra thực tế
const isAdmin = ref(true) // Tạm thời luôn là true để test giao diện

// Data state
const categories = ref([])
const loading = ref(true)
const error = ref(null)

// Filter state
const filters = reactive({
  name: '',
  isActive: null,
  createdAt: null
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

// Load categories on component mount
onMounted(async () => {
  await loadCategories()
})

async function loadCategories() {
  loading.value = true
  error.value = null
  try {
    categories.value = await incomeCategories.getAll(filters)
  } catch (err) {
    error.value = 'Không thể tải danh mục. Vui lòng thử lại sau.'
    console.error('Error loading categories:', err)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.name = ''
  filters.isActive = null
  filters.createdAt = null
  loadCategories()
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
      await incomeCategories.update(modalCategory.id, {
        name: modalCategory.name,
        description: modalCategory.description,
        isActive: modalCategory.isActive
      })
      showNotification('Cập nhật danh mục thành công')
    } else {
      await incomeCategories.create({
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
    await incomeCategories.delete(categoryToDelete.value.id)
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

function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const statusOptions = [
  { value: null, label: 'Tất cả' },
  { value: true, label: 'Hoạt động' },
  { value: false, label: 'Không hoạt động' }
]
</script>

<template>
  <AppLayout>
    <template #page-title>Danh mục thu nhập</template>
    
    <div class="content-container">
      <div class="page-header">
        <div>
          <h2 class="page-title">Quản lý danh mục thu nhập</h2>
          <p class="page-description">Quản lý các danh mục phân loại thu nhập của doanh nghiệp</p>
        </div>
        
        <button 
          @click="openAddModal"
          class="btn-add"
        >
          <i class="fas fa-plus"></i>
          Thêm mới
        </button>
      </div>
      
      <!-- Filter Form -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="filter-title">
            <i class="fas fa-filter"></i>
            Bộ lọc tìm kiếm
          </h3>
        </div>
        
        <div class="filter-content">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="form-label">Tên danh mục</label>
              <div class="input-icon-wrapper">
                <i class="fas fa-search input-icon-left"></i>
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
                <i class="fas fa-toggle-on input-icon-left"></i>
                <select v-model="filters.isActive" class="form-select has-icon">
                  <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>
            
            <div class="filter-item">
              <label class="form-label">Ngày tạo</label>
              <div class="input-icon-wrapper">
                <i class="fas fa-calendar-alt input-icon-left"></i>
                <input
                  v-model="filters.createdAt"
                  type="date"
                  class="form-input has-icon"
                />
              </div>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilters" class="btn-outline">
              <i class="fas fa-sync-alt"></i>
              Đặt lại
            </button>
            <button @click="loadCategories" class="btn-primary">
              <i class="fas fa-search"></i>
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
      
      <!-- Categories Table -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="fas fa-list-ul"></i>
            Danh sách danh mục
          </h3>
          <span class="result-count">{{ categories.length }} danh mục</span>
        </div>
        
        <div v-if="loading" class="card-empty-state">
          <div class="loading-spinner"></div>
          <p>Đang tải dữ liệu...</p>
        </div>
        
        <div v-else-if="error" class="card-empty-state error">
          <i class="fas fa-exclamation-circle error-icon"></i>
          <p>{{ error }}</p>
          <button @click="loadCategories" class="btn-primary">
            <i class="fas fa-sync-alt"></i>
            Thử lại
          </button>
        </div>
        
        <div v-else-if="categories.length === 0" class="card-empty-state">
          <i class="fas fa-folder-open empty-icon"></i>
          <p>Không tìm thấy danh mục thu nhập nào</p>
          <p class="empty-description">Hãy thêm danh mục mới hoặc điều chỉnh bộ lọc</p>
        </div>
        
        <div v-else class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>Tên danh mục</th>
                <th>Mô tả</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th class="text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="category in categories" :key="category.id">
                <td class="font-medium">{{ category.name }}</td>
                <td class="description-cell">{{ category.description || '–' }}</td>
                <td>
                  <span :class="['status-badge', category.isActive ? 'active' : 'inactive']">
                    <i :class="[
                      category.isActive ? 'fas fa-check-circle' : 'fas fa-times-circle',
                      'badge-icon'
                    ]"></i>
                    {{ category.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                  </span>
                </td>
                <td>{{ formatDate(category.createdAt) }}</td>
                <td class="actions-cell">
                  <div class="action-buttons">
                    <button 
                      @click="openEditModal(category)"
                      class="btn-icon btn-edit"
                      title="Chỉnh sửa"
                    >
                      <i class="fas fa-edit"></i>
                    </button>
                    <button 
                      @click="confirmDelete(category)"
                      class="btn-icon btn-delete"
                      title="Xóa"
                    >
                      <i class="fas fa-trash-alt"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
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
            <i class="fas fa-times"></i>
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
                <i class="fas fa-exclamation-circle"></i>
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
                <i v-if="saving" class="fas fa-circle-notch fa-spin"></i>
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
            <i class="fas fa-times"></i>
          </button>
        </div>
        
        <div class="modal-body text-center">
          <div class="icon-warning">
            <i class="fas fa-exclamation-triangle"></i>
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
              <i v-if="deleting" class="fas fa-circle-notch fa-spin"></i>
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
        notification.type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle',
        'toast-icon'
      ]"></i>
      <span>{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
/* Thiết kế container chung */
.content-container {
  padding: 0.5rem;
}

/* HEADER STYLES */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0 0.5rem;
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.card-header {
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 1rem;
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
}

/* FILTER STYLES */
.filter-container {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
  overflow: hidden;
}

.filter-header {
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.filter-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-title i {
  color: #4f46e5;
}

.filter-content {
  padding: 1rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 1rem;
  margin-bottom: 1rem;
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
  gap: 0.5rem;
  margin-top: 1rem;
}

/* FORM STYLES */
.form-group {
  margin-bottom: 1.25rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.375rem;
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
  padding: 0.625rem;
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
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
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
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.1);
}

.form-error {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: #ef4444;
  font-size: 0.75rem;
  margin-top: 0.375rem;
}

.input-icon-wrapper {
  position: relative;
}

.input-icon-left {
  position: absolute;
  top: 50%;
  left: 0.75rem;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.help-text {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

/* TABLE STYLES */
.table-responsive {
  overflow-x: auto;
}

.data-table {
  min-width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #6b7280;
  background-color: #f9fafb;
  letter-spacing: 0.05em;
}

.data-table tr:hover {
  background-color: #f9fafb;
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
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-badge.active {
  background-color: #dcfce7;
  color: #15803d;
}

.status-badge.inactive {
  background-color: #fee2e2;
  color: #b91c1c;
}

.badge-icon {
  margin-right: 0.25rem;
}

/* ACTION BUTTONS */
.actions-cell {
  text-align: right;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  transition: all 0.2s ease;
}

.btn-edit {
  background-color: #e0e7ff;
  color: #4f46e5;
}

.btn-edit:hover {
  background-color: #c7d2fe;
}

.btn-delete {
  background-color: #fee2e2;
  color: #ef4444;
}

.btn-delete:hover {
  background-color: #fecaca;
}

/* BUTTON STYLES */
.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background-color: #4f46e5;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
}

.btn-add:hover {
  background-color: #4338ca;
  transform: translateY(-1px);
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background-color: #4f46e5;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
}

.btn-primary:hover {
  background-color: #4338ca;
}

.btn-primary:disabled {
  background-color: #818cf8;
  cursor: not-allowed;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background-color: white;
  color: #4b5563;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-outline:hover {
  background-color: #f9fafb;
  border-color: #9ca3af;
}

.btn-outline:disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.btn-danger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background-color: #ef4444;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
}

.btn-danger:hover {
  background-color: #dc2626;
}

.btn-danger:disabled {
  background-color: #fca5a5;
  cursor: not-allowed;
}

.btn-close {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  color: #4b5563;
}

/* MODAL STYLES */
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
  z-index: 50;
  padding: 1rem;
}

.modal-container {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  width: 100%;
  max-width: 28rem;
  max-height: 90vh;
  overflow-y: auto;
  animation: modal-appear 0.3s ease-out forwards;
}

.modal-confirm {
  max-width: 24rem;
}

.modal-header {
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
}

.modal-body {
  padding: 1rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}

.icon-warning {
  font-size: 2rem;
  color: #f59e0b;
  margin-bottom: 0.5rem;
}

.confirm-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.5rem;
}

.confirm-message {
  font-size: 0.875rem;
  color: #374151;
}

.toast-notification {
  position: fixed;
  bottom: 1rem;
  right: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 0.375rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: white;
  z-index: 50;
}

.toast-notification.success {
  background-color: #4caf50;
}

.toast-notification.error {
  background-color: #f44336;
}

.toast-icon {
  font-size: 1.25rem;
}

@keyframes modal-appear {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
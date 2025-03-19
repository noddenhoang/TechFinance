<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import AppLayout from '../../components/layouts/AppLayout.vue'
import { incomeCategories } from '../../api/incomeCategories'
import { useAuthStore } from '../../stores/auth'

// Store và xác thực
const auth = useAuthStore()

// Sửa lại logic kiểm tra admin - BỎ HARDCODE VÀ KIỂM TRA ĐÚNG CẤU TRÚC
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

// Log để gỡ lỗi nếu cần
console.log('User authorities:', auth.user?.authorities);
console.log('Is admin:', isAdmin.value);

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
            
            <div class="filter-item">
              <label class="form-label">Ngày tạo</label>
              <div class="input-icon-wrapper">
                <i class="bi bi-calendar-date input-icon-left"></i>
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
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
            </button>
            <button @click="loadCategories" class="btn-primary">
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
                <td>{{ formatDate(category.createdAt) }}</td>
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
  padding: 0.75rem 1.25rem;
  background-color: #4f46e5;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.btn-primary:hover {
  background-color: #4338ca;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.btn-primary:disabled {
  background-color: #818cf8;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background-color: white;
  color: #4b5563;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.btn-outline:hover {
  background-color: #f8fafc;
  border-color: #9ca3af;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.btn-outline:disabled {
  color: #9ca3af;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-danger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background-color: #ef4444;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.btn-danger:hover {
  background-color: #dc2626;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.btn-danger:disabled {
  background-color: #fca5a5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
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
  transition: color 0.2s ease;
  width: 2rem;
  height: 2rem;
  padding: 0;
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
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  padding: 1rem;
  backdrop-filter: blur(2px);
}

.modal-container {
  background-color: white;
  border-radius: 0.75rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 30rem;
  max-height: 90vh;
  overflow-y: auto;
  animation: modal-appear 0.3s ease-out forwards;
}

.modal-confirm {
  max-width: 26rem;
}

.modal-header {
  padding: 1.25rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8fafc;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
}

.modal-body {
  padding: 1.5rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.modal-actions button {
  min-width: 6rem;
}

.icon-warning {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.icon-warning i {
  font-size: 3rem;
  color: #f59e0b;
}

.confirm-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.75rem;
}

.confirm-message {
  font-size: 0.938rem;
  color: #374151;
  line-height: 1.5;
}

.text-center {
  text-align: center;
}

/* CARD EMPTY STATES */
.card-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.empty-icon, .error-icon {
  font-size: 3rem;
  color: #9ca3af;
  margin-bottom: 1rem;
}

.error-icon {
  color: #ef4444;
}

.empty-description {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.5rem;
}

.loading-spinner {
  border: 3px solid #e5e7eb;
  border-top: 3px solid #4f46e5;
  border-radius: 50%;
  width: 2.5rem;
  height: 2.5rem;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

/* NOTIFICATION TOAST */
.toast-notification {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  padding: 0.875rem 1.25rem;
  border-radius: 0.5rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 0.625rem;
  font-size: 0.938rem;
  font-weight: 500;
  color: white;
  z-index: 50;
  animation: toast-slide-in 0.3s ease-out forwards;
}

.toast-notification.success {
  background-color: #10b981;
}

.toast-notification.error {
  background-color: #ef4444;
}

/* CHECKBOX STYLES */
.checkbox-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.checkbox-input {
  width: 1.125rem;
  height: 1.125rem;
  margin-right: 0.5rem;
  accent-color: #4f46e5;
}

.checkbox-label {
  font-size: 0.938rem;
  color: #374151;
}

/* BOOTSTRAP ICONS STYLES */
.bi {
  display: inline-block;
  vertical-align: -0.125em;
  width: 1em;
  height: 1em;
  line-height: 1;
  font-size: inherit;
}

/* Điều chỉnh icon căn giữa trong các nút */
.btn-primary .bi,
.btn-outline .bi,
.btn-danger .bi,
.btn-close .bi {
  vertical-align: -0.125em;
  position: relative;
  top: -0.05em;
}

/* Điều chỉnh icon trong status badge */
.status-badge .bi {
  position: relative;
  top: -0.05em;
}

/* Điều chỉnh icon trong form error */
.form-error .bi {
  position: relative;
  top: -0.05em;
}

/* Điều chỉnh icon trong toast notification */
.toast-notification .bi {
  position: relative;
  top: -0.05em;
}

/* Điều chỉnh icon trong empty state */
.empty-icon, 
.error-icon {
  position: relative;
  top: 0.125em;
}

/* Điều chỉnh icon trong warning */
.icon-warning .bi {
  position: relative;
  top: 0;
}

/* Điều chỉnh icon trong action button */
.btn-icon .bi {
  font-size: 1.1em;
  position: relative;
  top: 0;
}

/* Chỉnh spinner alignment */
.spinner {
  display: inline-block;
  position: relative;
  top: -0.05em;
}

/* Chỉnh card-title alignment */
.card-title .bi {
  position: relative;
  top: -0.05em;
}

/* ANIMATIONS */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes modal-appear {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes toast-slide-in {
  from {
    opacity: 0;
    transform: translateX(50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
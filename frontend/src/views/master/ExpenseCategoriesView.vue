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

// Define required fields
const requiredFields = ['name'];

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
      closeModal()
      await loadCategories(pagination.currentPage)
    } else {
      await expenseCategories.create({
        name: modalCategory.name,
        description: modalCategory.description,
        isActive: modalCategory.isActive
      })
      showNotification('Tạo danh mục mới thành công')
      closeModal()
      
      // Cập nhật sorting để hiển thị item mới nhất lên đầu
      sorting.field = 'id';
      sorting.direction = 'desc';
      
      // Load lại từ trang đầu tiên để xem item vừa tạo
      await loadCategories(0);
    }
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
    if (categories.value.length == 1 && pagination.currentPage > 0) {
       loadCategories(pagination.currentPage - 1)
    } else {
      await loadCategories(pagination.currentPage)
    }
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
                <th v-if="isAdmin" class="action-column text-center">Thao tác</th>
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
                <td v-if="isAdmin" class="action-column text-center">
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
              <label class="form-label required required-field">
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
/* Component-specific styles only */
/* All common styles have been moved to assets/styles/common.css */

/* Add this in your component's <style> section */
.required-field::after {
  content: " *";
  color: red;
}
</style>
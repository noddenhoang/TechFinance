<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import AppLayout from '../../components/layouts/AppLayout.vue'
import { incomeCategories } from '../../api/incomeCategories'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

// Try different approaches to check for admin role
// Approach 1: If auth store has a roles array
const isAdmin = computed(() => {
  // Check if roles exist in the auth store
  if (auth.roles) {
    return auth.roles.includes('admin')
  }
  // Check if user object exists and has roles
  if (auth.user && auth.user.roles) {
    return auth.user.roles.includes('admin')
  }
  // Default to false if we can't determine the role
  return false
})

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
    error.value = 'Failed to load categories'
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
    modalErrors.name = 'Name is required'
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
      showNotification('Category updated successfully')
    } else {
      await incomeCategories.create({
        name: modalCategory.name,
        description: modalCategory.description,
        isActive: modalCategory.isActive
      })
      showNotification('Category created successfully')
    }
    closeModal()
    await loadCategories()
  } catch (err) {
    if (err.response?.data?.message?.includes('already exists')) {
      modalErrors.name = 'A category with this name already exists'
    } else {
      showNotification(err.response?.data?.message || 'An error occurred while saving', 'error')
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
    showNotification('Category deleted successfully')
    closeDeleteModal()
    await loadCategories()
  } catch (err) {
    showNotification('Failed to delete category', 'error')
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
    
    <div class="content-box mb-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold">Quản lý danh mục thu nhập</h2>
        <button 
          v-if="isAdmin" 
          @click="openAddModal"
          class="btn-primary flex items-center gap-2"
        >
          <i class="fas fa-plus"></i>
          Thêm mới
        </button>
      </div>
      
      <!-- Filter Form -->
      <div class="bg-gray-50 border border-gray-200 rounded-md p-4 mb-6">
        <h3 class="text-lg font-medium mb-3">Bộ lọc tìm kiếm</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Tên danh mục</label>
            <input
              v-model="filters.name"
              type="text"
              class="form-input w-full"
              placeholder="Nhập tên để tìm kiếm"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Trạng thái</label>
            <select v-model="filters.isActive" class="form-select w-full">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Ngày tạo</label>
            <input
              v-model="filters.createdAt"
              type="date"
              class="form-input w-full"
            />
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button @click="resetFilters" class="btn-secondary mr-2">
            <i class="fas fa-sync-alt mr-1"></i> Đặt lại
          </button>
          <button @click="loadCategories" class="btn-primary">
            <i class="fas fa-search mr-1"></i> Tìm kiếm
          </button>
        </div>
      </div>

      <!-- Categories Table -->
      <div class="bg-white border border-gray-200 rounded-md shadow-sm overflow-hidden">
        <div v-if="loading" class="p-8 text-center">
          <div class="inline-block animate-spin h-8 w-8 border-4 border-gray-300 border-t-blue-600 rounded-full"></div>
          <p class="mt-2 text-gray-600">Đang tải dữ liệu...</p>
        </div>

        <div v-else-if="error" class="p-8 text-center">
          <div class="text-red-500 mb-2">
            <i class="fas fa-exclamation-circle text-xl"></i>
          </div>
          <p class="text-red-600">{{ error }}</p>
          <button @click="loadCategories" class="btn-primary mt-4">Thử lại</button>
        </div>

        <div v-else-if="categories.length === 0" class="p-8 text-center">
          <div class="text-gray-400 mb-2">
            <i class="fas fa-folder-open text-3xl"></i>
          </div>
          <p class="text-gray-600">Không tìm thấy danh mục thu nhập nào</p>
        </div>

        <table v-else class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Tên danh mục
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Mô tả
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Trạng thái
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Ngày tạo
              </th>
              <th v-if="isAdmin" class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="category in categories" :key="category.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="font-medium text-gray-900">{{ category.name }}</div>
              </td>
              <td class="px-6 py-4">
                <div class="text-sm text-gray-500">{{ category.description || '-' }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="[
                    'px-2 inline-flex text-xs leading-5 font-semibold rounded-full',
                    category.isActive 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-red-100 text-red-800'
                  ]"
                >
                  {{ category.isActive ? 'Hoạt động' : 'Không hoạt động' }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-500">{{ formatDate(category.createdAt) }}</div>
              </td>
              <td v-if="isAdmin" class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button 
                  @click="openEditModal(category)"
                  class="text-indigo-600 hover:text-indigo-900 mr-3"
                >
                  <i class="fas fa-edit"></i> Sửa
                </button>
                <button 
                  @click="confirmDelete(category)"
                  class="text-red-600 hover:text-red-900"
                >
                  <i class="fas fa-trash-alt"></i> Xóa
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div class="bg-white rounded-lg shadow-lg w-full max-w-md mx-auto p-6">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-medium text-gray-900">
            {{ editMode ? 'Chỉnh sửa danh mục thu nhập' : 'Thêm danh mục thu nhập mới' }}
          </h3>
          <button @click="closeModal" class="text-gray-400 hover:text-gray-500">
            <i class="fas fa-times"></i>
          </button>
        </div>
        
        <form @submit.prevent="saveCategory">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Tên danh mục <span class="text-red-500">*</span>
            </label>
            <input
              v-model="modalCategory.name"
              type="text"
              required
              class="form-input w-full"
              placeholder="Nhập tên danh mục"
            />
            <p v-if="modalErrors.name" class="mt-1 text-sm text-red-600">{{ modalErrors.name }}</p>
          </div>
          
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Mô tả
            </label>
            <textarea
              v-model="modalCategory.description"
              rows="3"
              class="form-textarea w-full"
              placeholder="Nhập mô tả cho danh mục này"
            ></textarea>
          </div>
          
          <div class="mb-6">
            <div class="flex items-center">
              <input
                v-model="modalCategory.isActive"
                type="checkbox"
                id="isActive"
                class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
              />
              <label for="isActive" class="ml-2 block text-sm text-gray-900">
                Đang hoạt động
              </label>
            </div>
          </div>
          
          <div class="flex justify-end">
            <button
              type="button"
              @click="closeModal"
              class="btn-secondary mr-2"
            >
              Hủy
            </button>
            <button
              type="submit"
              :disabled="saving"
              class="btn-primary"
            >
              <i v-if="saving" class="fas fa-spinner fa-spin mr-1"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div class="bg-white rounded-lg shadow-lg w-full max-w-md mx-auto p-6">
        <div class="mb-4">
          <h3 class="text-lg font-medium text-gray-900">Xác nhận xóa</h3>
          <p class="text-sm text-gray-500 mt-2">
            Bạn có chắc chắn muốn xóa danh mục "{{ categoryToDelete?.name }}"?
            Hành động này không thể hoàn tác.
          </p>
        </div>
        
        <div class="flex justify-end">
          <button
            @click="closeDeleteModal"
            class="btn-secondary mr-2"
          >
            Hủy
          </button>
          <button
            @click="deleteCategory"
            :disabled="deleting"
            class="btn-danger"
          >
            <i v-if="deleting" class="fas fa-spinner fa-spin mr-1"></i>
            {{ deleting ? 'Đang xóa...' : 'Xóa' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div 
      v-if="notification.show"
      :class="[
        'fixed bottom-4 right-4 p-4 rounded-md shadow-lg text-white max-w-md z-50',
        notification.type === 'success' ? 'bg-green-500' : 'bg-red-500'
      ]"
    >
      <div class="flex items-center">
        <i :class="[
          'mr-2',
          notification.type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'
        ]"></i>
        <span>{{ notification.message }}</span>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.content-box {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  @apply bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md font-medium;
}

.btn-secondary {
  @apply bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-md font-medium;
}

.btn-danger {
  @apply bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md font-medium;
}

.form-input,
.form-select,
.form-textarea {
  @apply mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-500 focus:ring-opacity-50;
}
</style>
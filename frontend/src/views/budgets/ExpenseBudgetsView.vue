<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useAuthStore } from '../../stores/auth';
import AppLayout from '../../components/layouts/AppLayout.vue';
import { expenseBudgets } from '../../api/expenseBudgets';
import { expenseCategories } from '../../api/expenseCategories';

// Auth store để kiểm tra quyền hạn
const auth = useAuthStore();

// Dữ liệu và trạng thái
const budgetsList = ref([]);
const categories = ref([]);
const isLoading = ref(true);

// Filter
const filters = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  categoryId: null
});

// Pagination
const pagination = reactive({
  currentPage: 0,
  totalPages: 0,
  totalItems: 0,
  pageSize: 10
});

// Sorting
const sorting = reactive({
  field: 'year',
  direction: 'desc'
});

// Modal states
const showBudgetDetailsModal = ref(false);
const currentBudget = ref({});

// Notification
const notification = reactive({
  show: false,
  message: '',
  type: 'success'
});

// Computed properties
const isAdmin = computed(() => {
  if (!auth.user) return false;
  
  if (auth.user.role === 'ADMIN' || auth.user.role === 'ROLE_ADMIN') {
    return true;
  }
  
  // Kiểm tra authorities
  if (auth.user.authorities) {
    return auth.user.authorities.some(authority => 
      typeof authority === 'string' 
        ? authority.includes('ADMIN') 
        : authority.authority && authority.authority.includes('ADMIN')
    );
  }
  
  return false;
});

const months = computed(() => [
  { id: 1, name: 'Tháng 1' },
  { id: 2, name: 'Tháng 2' },
  { id: 3, name: 'Tháng 3' },
  { id: 4, name: 'Tháng 4' },
  { id: 5, name: 'Tháng 5' },
  { id: 6, name: 'Tháng 6' },
  { id: 7, name: 'Tháng 7' },
  { id: 8, name: 'Tháng 8' },
  { id: 9, name: 'Tháng 9' },
  { id: 10, name: 'Tháng 10' },
  { id: 11, name: 'Tháng 11' },
  { id: 12, name: 'Tháng 12' }
]);

const years = computed(() => {
  const currentYear = new Date().getFullYear();
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

// Methods
const loadData = async () => {
  isLoading.value = true;
  try {
    // Load danh sách danh mục chi tiêu
    const categoriesResult = await expenseCategories.getAll();
    categories.value = categoriesResult.content || categoriesResult;
    
    // Load danh sách ngân sách chi tiêu với filter
    const result = await expenseBudgets.getAll(
      filters,
      pagination.currentPage,
      pagination.pageSize,
      sorting.field,
      sorting.direction
    );
    
    budgetsList.value = result.content || [];
    
    // Cập nhật thông tin phân trang
    if (result.page) {
      pagination.currentPage = result.page.page || 0;
      pagination.totalPages = result.page.totalPages || 0;
      pagination.totalItems = result.page.totalElements || result.page.totalElement || 0;
    }
  } catch (error) {
    console.error('Error loading budgets:', error);
    showNotification('Không thể tải dữ liệu ngân sách. Vui lòng thử lại sau.', 'error');
  } finally {
    isLoading.value = false;
  }
};

const refreshBudgets = async () => {
  try {
    await expenseBudgets.refresh(filters.year, filters.month);
    showNotification('Ngân sách đã được cập nhật tự động dựa trên chi tiêu thực tế', 'success');
    loadData();
  } catch (error) {
    console.error('Error refreshing budgets:', error);
    showNotification('Không thể cập nhật ngân sách. Vui lòng thử lại sau.', 'error');
  }
};

const applyFilter = () => {
  pagination.currentPage = 0;
  loadData();
};

const resetFilter = () => {
  filters.year = new Date().getFullYear();
  filters.month = null;
  filters.categoryId = null;
  pagination.currentPage = 0;
  loadData();
};

const sortBy = (field) => {
  if (sorting.field === field) {
    sorting.direction = sorting.direction === 'asc' ? 'desc' : 'asc';
  } else {
    sorting.field = field;
    sorting.direction = 'asc';
  }
  loadData();
};

const goToPage = (page) => {
  if (page >= 0 && page < pagination.totalPages) {
    pagination.currentPage = page;
    loadData();
  }
};

const openBudgetDetailsModal = (budget) => {
  currentBudget.value = { ...budget };
  showBudgetDetailsModal.value = true;
};

const showNotification = (message, type = 'success') => {
  notification.message = message;
  notification.type = type;
  notification.show = true;
  
  setTimeout(() => {
    notification.show = false;
  }, 3000);
};

const formatNumber = (value) => {
  if (!value) return '0';
  return new Intl.NumberFormat('vi-VN').format(value);
};

// Lifecycle hooks
onMounted(() => {
  loadData();
});
</script>

<template>
  <AppLayout>
    <template #page-title>Ngân sách chi tiêu</template>
    
    <div class="content-container">
      <!-- Filter Section -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel-fill"></i> Bộ lọc ngân sách
          </h3>
        </div>
        <div class="filter-content">
          <div class="filter-grid">
            <div class="form-group">
              <label class="form-label">Năm</label>
              <select v-model="filters.year" class="form-select">
                <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Tháng</label>
              <select v-model="filters.month" class="form-select">
                <option :value="null">Tất cả các tháng</option>
                <option v-for="month in months" :key="month.id" :value="month.id">{{ month.name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Danh mục</label>
              <select v-model="filters.categoryId" class="form-select">
                <option :value="null">Tất cả danh mục</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
              </select>
            </div>
          </div>
          
          <div class="filter-actions">
            <button @click="resetFilter" class="btn-secondary">
              <i class="bi bi-arrow-counterclockwise"></i> Đặt lại
            </button>
            <button @click="applyFilter" class="btn-primary">
              <i class="bi bi-search"></i> Áp dụng
            </button>
          </div>
        </div>
      </div>
      
      <!-- Main Content -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="bi bi-cash-stack"></i> Ngân sách chi tiêu
          </h3>
          
          <div class="action-buttons">
            <button @click="refreshBudgets" class="btn-outline">
              <i class="bi bi-arrow-repeat"></i> Cập nhật tự động
            </button>
          </div>
        </div>
        
        <!-- Loading State -->
        <div v-if="isLoading" class="card-empty-state">
          <div class="loading-spinner"></div>
          <p>Đang tải dữ liệu...</p>
        </div>
        
        <!-- Empty State -->
        <div v-else-if="budgetsList.length === 0" class="card-empty-state">
          <i class="bi bi-cash-stack empty-icon"></i>
          <h4>Không có dữ liệu ngân sách</h4>
          <p class="empty-description">Hệ thống sẽ tự động cập nhật ngân sách dựa trên chi tiêu thực tế đã thanh toán.</p>
        </div>
        
        <!-- Data Table -->
        <div v-else class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th @click="sortBy('id')" class="sortable">
                  ID 
                  <i v-if="sorting.field === 'id'" 
                    :class="[
                      'sort-icon',
                      'bi',
                      sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
                    ]">
                  </i>
                </th>
                <th @click="sortBy('categoryName')" class="sortable">
                  Danh mục
                  <i v-if="sorting.field === 'categoryName'" 
                    :class="[
                      'sort-icon',
                      'bi',
                      sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
                    ]">
                  </i>
                </th>
                <th @click="sortBy('year')" class="sortable">
                  Năm
                  <i v-if="sorting.field === 'year'" 
                    :class="[
                      'sort-icon',
                      'bi',
                      sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
                    ]">
                  </i>
                </th>
                <th @click="sortBy('month')" class="sortable">
                  Tháng
                  <i v-if="sorting.field === 'month'" 
                    :class="[
                      'sort-icon',
                      'bi',
                      sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
                    ]">
                  </i>
                </th>
                <th @click="sortBy('amount')" class="sortable text-right">
                  Số tiền
                  <i v-if="sorting.field === 'amount'" 
                    :class="[
                      'sort-icon',
                      'bi',
                      sorting.direction === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
                    ]">
                  </i>
                </th>
                <th class="action-column">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="budget in budgetsList" :key="budget.id">
                <td>{{ budget.id }}</td>
                <td class="font-medium">{{ budget.categoryName }}</td>
                <td>{{ budget.year }}</td>
                <td>{{ months.find(m => m.id === budget.month)?.name }}</td>
                <td class="text-right font-medium">{{ formatNumber(budget.amount) }} ₫</td>
                <td>
                  <div class="action-buttons">
                    <button @click="openBudgetDetailsModal(budget)" class="btn-icon view-btn" title="Xem chi tiết">
                      <i class="bi bi-eye"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Pagination -->
        <div v-if="!isLoading && budgetsList.length > 0" class="pagination-container">
          <div class="pagination-info">
            Hiển thị {{ pagination.totalItems === 0 ? 0 : pagination.currentPage * pagination.pageSize + 1 }} đến 
            {{ Math.min((pagination.currentPage + 1) * pagination.pageSize, pagination.totalItems) }} 
            trên tổng số {{ pagination.totalItems }} ngân sách
          </div>
          
          <div class="pagination-controls">
            <button 
              @click="goToPage(0)"
              class="pagination-btn"
              :disabled="pagination.currentPage === 0">
              <i class="bi bi-chevron-double-left"></i>
            </button>
            
            <button 
              @click="goToPage(pagination.currentPage - 1)"
              class="pagination-btn"
              :disabled="pagination.currentPage === 0">
              <i class="bi bi-chevron-left"></i>
            </button>
            
            <span class="pagination-text">
              {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
            </span>
            
            <button 
              @click="goToPage(pagination.currentPage + 1)"
              class="pagination-btn"
              :disabled="pagination.currentPage >= pagination.totalPages - 1">
              <i class="bi bi-chevron-right"></i>
            </button>
            
            <button 
              @click="goToPage(pagination.totalPages - 1)"
              class="pagination-btn"
              :disabled="pagination.currentPage >= pagination.totalPages - 1">
              <i class="bi bi-chevron-double-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Budget Details Modal -->
    <div v-if="showBudgetDetailsModal" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Chi tiết ngân sách</h3>
          <button @click="showBudgetDetailsModal = false" class="btn-close">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="budget-avatar">
            <i class="bi bi-cash-stack"></i>
          </div>
          
          <h4 class="budget-amount text-center">{{ formatNumber(currentBudget.amount) }} ₫</h4>
          <p class="budget-period text-center">{{ months.find(m => m.id === currentBudget.month)?.name }} - {{ currentBudget.year }}</p>
          
          <div class="form-group">
            <label class="form-label">Danh mục</label>
            <div class="detail-value">{{ currentBudget.categoryName }}</div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <div class="detail-value">{{ currentBudget.notes || 'Không có ghi chú' }}</div>
          </div>
          
          <div class="modal-actions">
            <button @click="showBudgetDetailsModal = false" class="btn-secondary">Đóng</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Toast Notification -->
    <div 
      v-if="notification.show"
      :class="['toast-notification', notification.type]"
    >
      <i :class="[
        notification.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'
      ]"></i>
      <span>{{ notification.message }}</span>
    </div>
  </AppLayout>
</template>

<style scoped>
.budget-avatar {
  background-color: #fee2e2;
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.budget-avatar i {
  font-size: 2rem;
  color: #ef4444;
}

.budget-amount {
  font-size: 1.5rem;
  font-weight: 600;
  color: #ef4444;
  text-align: center;
  margin-bottom: 0.5rem;
}

.budget-period {
  font-size: 1rem;
  color: #6b7280;
  text-align: center;
  margin-bottom: 1rem;
}
</style>
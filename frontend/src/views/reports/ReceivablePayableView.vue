<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue'

// Current year for filter selection
const currentYear = new Date().getFullYear();

// Generate years from 2020 to current year
const years = computed(() => {
  const yearList = [];
  for (let i = 2020; i <= currentYear; i++) {
    yearList.push(i);
  }
  return yearList;
});

// Filters
const filters = reactive({
  year: currentYear
});

// Change year
const changeYear = () => {
  // Will be implemented to load data when the report is fully developed
  console.log('Year changed to:', filters.year);
  loadData();
};

// Will load data based on selected year
const loadData = () => {
  // To be implemented
  console.log('Loading data for year:', filters.year);
};

// Initialize on component mount
onMounted(() => {
  // Will initialize data when the report is fully developed
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo phải thu/trả</template>
    
    <div class="content-box">
      <h2>Báo cáo tiền phải thu, phải trả & lỗi giao dịch</h2>
      
      <!-- Filter container -->
      <div class="filter-container">
        <div class="filter-header">
          <h3 class="card-title">
            <i class="bi bi-funnel-fill"></i> Bộ lọc báo cáo
          </h3>
        </div>
        <div class="filter-content">
          <div class="filter-grid">
            <div class="form-group">
              <label class="form-label">Năm</label>
              <select v-model="filters.year" class="form-select" @change="changeYear">
                <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
              </select>
            </div>
          </div>
        </div>
      </div>
      
      <p>Trang này hiển thị tổng hợp số liệu về các khoản chưa thanh toán (receivables, payables) và lợi nhuận.</p>
      
      <!-- Dữ liệu báo cáo phải thu/trả sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Báo cáo phải thu/trả</p>
        <p>Năm đang chọn: {{ filters.year }}</p>
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

.content-box h2 {
  color: #111827;
  margin-top: 0;
  margin-bottom: 1rem;
}

.content-box p {
  color: #6b7280;
  line-height: 1.5;
}

.placeholder-content {
  margin-top: 2rem;
  padding: 2rem;
  background-color: #f9fafb;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  text-align: center;
}

/* Filter styles */
.filter-container {
  margin-top: 1.5rem;
  margin-bottom: 2rem;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.filter-header {
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin: 0;
}

.card-title i {
  margin-right: 0.5rem;
}

.filter-content {
  padding: 1.5rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
}

.form-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
}
</style> 
<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import AppLayout from '../../components/layouts/AppLayout.vue'
import { taxReports } from '../../api/taxReports';

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

// Data state
const taxData = ref({
  incomeTax: [
    { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
    { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
  ],
  expenseTax: [
    { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
    { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
  ],
  difference: [
    { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
    { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
  ]
});

// Loading and error states
const isLoading = ref(false);
const error = ref(null);

// Format currency
const formatCurrency = (value) => {
  if (value === 0 || value === null || value === undefined) return '- đ';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(value);
};

// Calculate totals for each row
const calculateTotals = (taxItems) => {
  return taxItems.map(item => {
    const total = item.q1 + item.q2 + item.q3 + item.q4;
    return { ...item, total };
  });
};

// Calculate column totals
const calculateColumnTotals = (taxItems) => {
  const totals = { type: 'Tổng', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 };
  
  taxItems.forEach(item => {
    totals.q1 += item.q1;
    totals.q2 += item.q2;
    totals.q3 += item.q3;
    totals.q4 += item.q4;
    totals.total += item.total;
  });
  
  return totals;
};

// Computed properties for totals
const incomeTaxWithTotals = computed(() => {
  return calculateTotals(taxData.value.incomeTax);
});

const expenseTaxWithTotals = computed(() => {
  return calculateTotals(taxData.value.expenseTax);
});

const differenceWithTotals = computed(() => {
  return calculateTotals(taxData.value.difference);
});

const incomeTaxTotals = computed(() => {
  return calculateColumnTotals(incomeTaxWithTotals.value);
});

const expenseTaxTotals = computed(() => {
  return calculateColumnTotals(expenseTaxWithTotals.value);
});

const differenceTaxTotals = computed(() => {
  return calculateColumnTotals(differenceWithTotals.value);
});

// Change year
const changeYear = () => {
  loadData();
};

// Load data from API
const loadData = async () => {
  isLoading.value = true;
  error.value = null;
  
  try {
    // Attempt to fetch data from the API
    const response = await taxReports.getTaxReport(filters.year);
    
    // Process the response data and ensure all numbers are properly converted
    if (response) {
      // Convert string values to numbers if needed
      const processNumericValues = (items) => {
        return items.map(item => ({
          type: item.type,
          q1: Number(item.q1) || 0,
          q2: Number(item.q2) || 0,
          q3: Number(item.q3) || 0,
          q4: Number(item.q4) || 0
        }));
      };
      
      // Create a structured copy of the data with numeric conversions
      taxData.value = {
        incomeTax: processNumericValues(response.incomeTax),
        expenseTax: processNumericValues(response.expenseTax),
        difference: processNumericValues(response.difference)
      };
      
      // Clear any previous error
      error.value = null;
    }
  } catch (err) {
    console.error('Error loading tax report data:', err);
    
    // More specific error messaging
    if (err.response && err.response.status === 500) {
      if (err.response.data && err.response.data.error) {
        error.value = `${err.response.data.error}`;
      } else {
        error.value = 'Lỗi máy chủ, đang hiển thị dữ liệu mô phỏng';
      }
    } else if (err.request) {
      error.value = 'Không thể kết nối đến máy chủ, đang hiển thị dữ liệu mô phỏng';
    } else {
      error.value = 'Lỗi trong khi tải dữ liệu, đang hiển thị dữ liệu mô phỏng';
    }
    
    // Always fallback to mock data if API call fails
    taxData.value = generateMockData();
    
    // Generate some sample data with non-zero values for demonstration
    const addSampleData = (item, index) => {
      // Sample quarterly values for demonstration
      const quarters = ['q1', 'q2', 'q3', 'q4'];
      quarters.forEach(quarter => {
        // Generate consistent but different values for each type and quarter
        const baseValue = (index + 1) * 1000000 * (quarters.indexOf(quarter) + 1);
        item[quarter] = index === 0 ? baseValue : baseValue * 0.3;
      });
      return item;
    };
    
    // Update the mock data with sample values
    taxData.value.incomeTax = taxData.value.incomeTax.map(addSampleData);
    taxData.value.expenseTax = taxData.value.expenseTax.map(addSampleData);
    
    // Calculate difference
    for (let i = 0; i < taxData.value.difference.length; i++) {
      ['q1', 'q2', 'q3', 'q4'].forEach(quarter => {
        taxData.value.difference[i][quarter] = 
          taxData.value.incomeTax[i][quarter] - taxData.value.expenseTax[i][quarter];
      });
    }
  } finally {
    isLoading.value = false;
  }
};

// Generate mock data for demonstration or if API fails
const generateMockData = () => {
  return {
    incomeTax: [
      { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
      { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 }
    ],
    expenseTax: [
      { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
      { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 }
    ],
    difference: [
      { type: 'GTGT', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 },
      { type: 'Khác', q1: 0, q2: 0, q3: 0, q4: 0, total: 0 }
    ]
  };
};

// Initialize on component mount
onMounted(() => {
  loadData();
});
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo thuế</template>
    
    <div class="content-box">
      <h2>Báo cáo thuế</h2>
      
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
      
      <!-- Loading indicator -->
      <div v-if="isLoading" class="loading-spinner-container">
        <div class="loading-spinner"></div>
        <p>Đang tải dữ liệu báo cáo thuế...</p>
      </div>
      
      <!-- Warning message when using fallback data -->
      <div v-if="error" class="warning-message">
        <i class="bi bi-exclamation-triangle"></i>
        <p>{{ error }}</p>
        <button @click="loadData" class="btn-primary">
          <i class="bi bi-arrow-repeat"></i> Thử lại
        </button>
      </div>
      
      <!-- Tax Report Content -->
      <div v-if="!isLoading" class="tax-report-container">
        <!-- Income Tax Section -->
        <div class="tax-section">
          <h3 class="section-title">Thuế thu nhập</h3>
          <div class="table-container">
            <table class="tax-table">
              <thead>
                <tr>
                  <th>Thuế</th>
                  <th>Q1</th>
                  <th>Q2</th>
                  <th>Q3</th>
                  <th>Q4</th>
                  <th>Tổng năm</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in incomeTaxWithTotals" :key="item.type">
                  <td>{{ item.type }}</td>
                  <td>{{ formatCurrency(item.q1) }}</td>
                  <td>{{ formatCurrency(item.q2) }}</td>
                  <td>{{ formatCurrency(item.q3) }}</td>
                  <td>{{ formatCurrency(item.q4) }}</td>
                  <td>{{ formatCurrency(item.total) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>{{ incomeTaxTotals.type }}</th>
                  <th>{{ formatCurrency(incomeTaxTotals.q1) }}</th>
                  <th>{{ formatCurrency(incomeTaxTotals.q2) }}</th>
                  <th>{{ formatCurrency(incomeTaxTotals.q3) }}</th>
                  <th>{{ formatCurrency(incomeTaxTotals.q4) }}</th>
                  <th>{{ formatCurrency(incomeTaxTotals.total) }}</th>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
        
        <!-- Tax Expense Section -->
        <div class="tax-section">
          <h3 class="section-title">Chi phí thuế</h3>
          <div class="table-container">
            <table class="tax-table">
              <thead>
                <tr>
                  <th>Thuế</th>
                  <th>Q1</th>
                  <th>Q2</th>
                  <th>Q3</th>
                  <th>Q4</th>
                  <th>Tổng năm</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in expenseTaxWithTotals" :key="item.type">
                  <td>{{ item.type }}</td>
                  <td>{{ formatCurrency(item.q1) }}</td>
                  <td>{{ formatCurrency(item.q2) }}</td>
                  <td>{{ formatCurrency(item.q3) }}</td>
                  <td>{{ formatCurrency(item.q4) }}</td>
                  <td>{{ formatCurrency(item.total) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>{{ expenseTaxTotals.type }}</th>
                  <th>{{ formatCurrency(expenseTaxTotals.q1) }}</th>
                  <th>{{ formatCurrency(expenseTaxTotals.q2) }}</th>
                  <th>{{ formatCurrency(expenseTaxTotals.q3) }}</th>
                  <th>{{ formatCurrency(expenseTaxTotals.q4) }}</th>
                  <th>{{ formatCurrency(expenseTaxTotals.total) }}</th>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
        
        <!-- Difference Section -->
        <div class="tax-section">
          <h3 class="section-title">Chênh lệch</h3>
          <div class="table-container">
            <table class="tax-table">
              <thead>
                <tr>
                  <th>Thuế</th>
                  <th>Q1</th>
                  <th>Q2</th>
                  <th>Q3</th>
                  <th>Q4</th>
                  <th>Tổng năm</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in differenceWithTotals" :key="item.type">
                  <td>{{ item.type }}</td>
                  <td>{{ formatCurrency(item.q1) }}</td>
                  <td>{{ formatCurrency(item.q2) }}</td>
                  <td>{{ formatCurrency(item.q3) }}</td>
                  <td>{{ formatCurrency(item.q4) }}</td>
                  <td>{{ formatCurrency(item.total) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <th>{{ differenceTaxTotals.type }}</th>
                  <th>{{ formatCurrency(differenceTaxTotals.q1) }}</th>
                  <th>{{ formatCurrency(differenceTaxTotals.q2) }}</th>
                  <th>{{ formatCurrency(differenceTaxTotals.q3) }}</th>
                  <th>{{ formatCurrency(differenceTaxTotals.q4) }}</th>
                  <th>{{ formatCurrency(differenceTaxTotals.total) }}</th>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
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

/* Loading and error states */
.loading-spinner-container,
.warning-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  text-align: center;
  margin-bottom: 1.5rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin-bottom: 1rem;
  border: 4px solid #e5e7eb;
  border-radius: 50%;
  border-top-color: #3b82f6;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.warning-message {
  color: #854d0e;
  background-color: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 8px;
  padding: 1rem;
}

.warning-message i {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #f59e0b;
}

.warning-message p {
  margin-bottom: 0.75rem;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  border: 1px solid #2563eb;
  background-color: #2563eb;
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
}

/* Tax Report Styles */
.tax-report-container {
  margin-top: 2rem;
}

.tax-section {
  margin-bottom: 2rem;
}

.section-title {
  color: #4b5563;
  font-size: 1.2rem;
  margin-bottom: 1rem;
  font-weight: 600;
}

.table-container {
  overflow-x: auto;
}

.tax-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.tax-table th,
.tax-table td {
  border: 1px solid #e5e7eb;
  padding: 0.75rem;
  text-align: right;
}

.tax-table th:first-child,
.tax-table td:first-child {
  text-align: left;
}

.tax-table thead th {
  background-color: #f3f4f6;
  color: #374151;
  font-weight: 600;
}

.tax-table tbody tr:nth-child(even) {
  background-color: #f9fafb;
}

.tax-table tfoot th {
  background-color: #f3f4f6;
  font-weight: 600;
}

@media (max-width: 768px) {
  .content-box {
    padding: 1rem;
  }
  
  .tax-table th,
  .tax-table td {
    padding: 0.5rem;
    font-size: 0.875rem;
  }
}
</style> 
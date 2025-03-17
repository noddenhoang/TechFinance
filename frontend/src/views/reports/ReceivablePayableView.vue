<script setup>
import AppLayout from '../../components/layouts/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo phải thu/trả</template>
    
    <div class="content-box">
      <h2>Báo cáo tiền phải thu, phải trả & lỗi giao dịch</h2>
      <p>Trang này hiển thị tổng hợp số liệu về các khoản chưa thanh toán (receivables, payables) và lợi nhuận.</p>
      
      <!-- Dữ liệu báo cáo phải thu/trả sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Báo cáo phải thu/trả</p>
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
</style> 
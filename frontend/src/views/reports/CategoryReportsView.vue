<script setup>
import AppLayout from '../../components/layouts/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo theo danh mục</template>
    
    <div class="content-box">
      <h2>Báo cáo phân tích theo danh mục</h2>
      <p>Trang này hiển thị báo cáo chi tiết theo từng danh mục thu nhập và chi phí, bao gồm phần trăm so với tổng số.</p>
      
      <!-- Dữ liệu báo cáo danh mục sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Báo cáo phân tích theo danh mục</p>
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
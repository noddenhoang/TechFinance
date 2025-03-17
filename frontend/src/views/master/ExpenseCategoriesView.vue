<script setup>
import AppLayout from '../../components/layouts/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <template #page-title>Danh mục chi phí</template>
    
    <div class="content-box">
      <h2>Quản lý danh mục chi phí</h2>
      <p>Trang này cho phép bạn quản lý các danh mục chi phí trong hệ thống.</p>
      
      <!-- Dữ liệu danh mục chi phí sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Danh sách và quản lý danh mục chi phí</p>
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
<script setup>
import AppLayout from '../../components/layouts/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo KH/NCC</template>
    
    <div class="content-box">
      <h2>Báo cáo theo khách hàng/nhà cung cấp</h2>
      <p>Trang này hiển thị báo cáo thống kê giao dịch dựa trên khách hàng và nhà cung cấp.</p>
      
      <!-- Dữ liệu báo cáo KH/NCC sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Báo cáo theo khách hàng/nhà cung cấp</p>
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
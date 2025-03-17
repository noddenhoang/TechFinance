<script setup>
import AppLayout from '../../components/layouts/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <template #page-title>Báo cáo tháng</template>
    
    <div class="content-box">
      <h2>Báo cáo tổng doanh thu/chi phí hằng tháng</h2>
      <p>Trang này hiển thị báo cáo tổng hợp về thu nhập và chi phí theo tháng, bao gồm phân tích lợi nhuận.</p>
      
      <!-- Dữ liệu báo cáo tháng sẽ được hiển thị ở đây -->
      <div class="placeholder-content">
        <p>Đang phát triển: Báo cáo tổng doanh thu/chi phí hằng tháng</p>
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
<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const isSidebarCollapsed = ref(false)

const logout = () => {
  authStore.logout()
  router.push('/login')
}

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}
</script>

<template>
  <div class="app-container">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
      <div class="sidebar-header">
        <h2 class="sidebar-title">TechFinance</h2>
        <button class="sidebar-toggle" @click="toggleSidebar">
          <span v-if="isSidebarCollapsed">›</span>
          <span v-else>‹</span>
        </button>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <h3 class="nav-title">Tổng quan</h3>
          <ul class="nav-items">
            <li class="nav-item">
              <router-link to="/dashboard" class="nav-link">
                <span class="nav-icon">📊</span>
                <span class="nav-text">Dashboard</span>
              </router-link>
            </li>
          </ul>
        </div>

        <div class="nav-section">
          <h3 class="nav-title">Dữ liệu Master</h3>
          <ul class="nav-items">
            <li class="nav-item">
              <router-link to="/income-categories" class="nav-link">
                <span class="nav-icon">📋</span>
                <span class="nav-text">Danh mục thu nhập</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/expense-categories" class="nav-link">
                <span class="nav-icon">📋</span>
                <span class="nav-text">Danh mục chi phí</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/customers" class="nav-link">
                <span class="nav-icon">👥</span>
                <span class="nav-text">Khách hàng</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/suppliers" class="nav-link">
                <span class="nav-icon">🏢</span>
                <span class="nav-text">Nhà cung cấp</span>
              </router-link>
            </li>
          </ul>
        </div>

        <div class="nav-section">
          <h3 class="nav-title">Quản lý giao dịch</h3>
          <ul class="nav-items">
            <li class="nav-item">
              <router-link to="/income-transactions" class="nav-link">
                <span class="nav-icon">💰</span>
                <span class="nav-text">Giao dịch thu nhập</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/expense-transactions" class="nav-link">
                <span class="nav-icon">💸</span>
                <span class="nav-text">Giao dịch chi phí</span>
              </router-link>
            </li>
          </ul>
        </div>

        <div class="nav-section">
          <h3 class="nav-title">Kế hoạch ngân sách</h3>
          <ul class="nav-items">
            <li class="nav-item">
              <router-link to="/income-budgets" class="nav-link">
                <span class="nav-icon">📈</span>
                <span class="nav-text">Ngân sách thu nhập</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/expense-budgets" class="nav-link">
                <span class="nav-icon">📉</span>
                <span class="nav-text">Ngân sách chi phí</span>
              </router-link>
            </li>
          </ul>
        </div>

        <div class="nav-section">
          <h3 class="nav-title">Báo cáo & Thống kê</h3>
          <ul class="nav-items">
            <li class="nav-item">
              <router-link to="/monthly-reports" class="nav-link">
                <span class="nav-icon">📝</span>
                <span class="nav-text">Báo cáo tháng</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/budget-overview" class="nav-link">
                <span class="nav-icon">💹</span>
                <span class="nav-text">Tổng quan ngân sách</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/category-reports" class="nav-link">
                <span class="nav-icon">📊</span>
                <span class="nav-text">Báo cáo theo danh mục</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/customer-supplier-reports" class="nav-link">
                <span class="nav-icon">🔍</span>
                <span class="nav-text">Báo cáo KH/NCC</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/tax-reports" class="nav-link">
                <span class="nav-icon">🧾</span>
                <span class="nav-text">Báo cáo thuế</span>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/receivable-payable" class="nav-link">
                <span class="nav-icon">💱</span>
                <span class="nav-text">Báo cáo phải thu/trả</span>
              </router-link>
            </li>
          </ul>
        </div>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-button" @click="logout">
          <span class="nav-icon">🚪</span>
          <span class="nav-text">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <header class="main-header">
        <div class="header-left">
          <h1 class="page-title">
            <slot name="page-title">Dashboard</slot>
          </h1>
        </div>
        <div class="user-area">
          <span class="user-name">{{ authStore.user?.fullName || 'Người dùng' }}</span>
        </div>
      </header>
      
      <div class="content-area">
        <slot></slot>
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
  background-color: #f1f5f9;
}

/* Sidebar styles */
.sidebar {
  width: 260px;
  background-color: #1e293b;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 70px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid #334155;
}

.sidebar-title {
  font-size: 1.25rem;
  margin: 0;
  color: #f8fafc;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar.collapsed .sidebar-title {
  display: none;
}

.sidebar-toggle {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-toggle:hover {
  color: #f8fafc;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 1rem 0;
}

.nav-section {
  margin-bottom: 1.5rem;
}

.nav-title {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
  padding: 0 1rem;
  margin-bottom: 0.5rem;
}

.sidebar.collapsed .nav-title {
  display: none;
}

.nav-items {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  margin-bottom: 0.25rem;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  color: #cbd5e1;
  text-decoration: none;
  transition: all 0.15s ease;
}

.nav-link:hover, .router-link-active {
  background-color: #334155;
  color: #f8fafc;
}

.nav-icon {
  margin-right: 0.75rem;
  font-size: 1.125rem;
  width: 1.5rem;
  text-align: center;
}

.sidebar.collapsed .nav-text {
  display: none;
}

.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid #334155;
}

.logout-button {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: #1e40af;
  color: white;
  border: none;
  border-radius: 0.375rem;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.logout-button:hover {
  background-color: #1e3a8a;
}

.sidebar.collapsed .logout-button .nav-text {
  display: none;
}

/* Main content styles */
.main-content {
  flex: 1;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  margin-left: 0;
  width: calc(100% - 260px);
}

.main-content.sidebar-collapsed {
  width: calc(100% - 70px);
  margin-left: 0;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background-color: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.page-title {
  color: #111827;
  margin: 0;
  font-size: 1.5rem;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-name {
  font-weight: 500;
  color: #4b5563;
}

.content-area {
  flex: 1;
  padding: 2rem;
  background-color: #f1f5f9;
  overflow-y: auto;
}

/* Responsive styles */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100%;
    z-index: 100;
    transform: translateX(-100%);
  }
  
  .sidebar.active {
    transform: translateX(0);
  }
  
  .main-content {
    margin-left: 0;
  }
}
</style> 
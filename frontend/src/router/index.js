import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { requiresAuth: true }
  },
  // Gemini AI Route
  {
    path: '/gemini',
    name: 'Gemini',
    component: () => import('../views/GeminiView.vue'),
    meta: { 
      requiresAuth: true,
      title: 'Trợ lý AI Tài Chính'
    }
  },
  // Master Data Routes
  {
    path: '/income-categories',
    name: 'IncomeCategories',
    component: () => import('../views/master/IncomeCategoriesView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/expense-categories',
    name: 'ExpenseCategories',
    component: () => import('../views/master/ExpenseCategoriesView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/customers',
    name: 'Customers',
    component: () => import('../views/master/CustomersView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/suppliers',
    name: 'Suppliers',
    component: () => import('../views/master/SuppliersView.vue'),
    meta: { requiresAuth: true }
  },
  // Transaction Routes
  {
    path: '/income-transactions',
    name: 'IncomeTransactions',
    component: () => import('../views/transactions/IncomeTransactionsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/expense-transactions',
    name: 'ExpenseTransactions',
    component: () => import('../views/transactions/ExpenseTransactionsView.vue'),
    meta: { requiresAuth: true }
  },
  // Budget Routes
  {
    path: '/income-budgets',
    name: 'IncomeBudgets',
    component: () => import('../views/budgets/IncomeBudgetsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/expense-budgets',
    name: 'ExpenseBudgets',
    component: () => import('../views/budgets/ExpenseBudgetsView.vue'),
    meta: { requiresAuth: true }
  },
  // Report Routes
  {
    path: '/monthly-reports',
    name: 'monthly-reports',
    component: () => import('../views/reports/MonthlyReportsView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Báo cáo - Dòng tiền'
    }
  },
  {
    path: '/budget-overview',
    name: 'BudgetOverview',
    component: () => import('../views/reports/BudgetOverviewView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/category-reports',
    name: 'CategoryReports',
    component: () => import('../views/reports/CategoryReportsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/customer-supplier-reports',
    name: 'CustomerSupplierReports',
    component: () => import('../views/reports/CustomerSupplierReportsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tax-reports',
    name: 'TaxReports',
    component: () => import('../views/reports/TaxReportsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/receivable-payable',
    name: 'ReceivablePayable',
    component: () => import('../views/reports/ReceivablePayableView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard to check authentication
router.beforeEach((to, from, next) => {
  const isAuthenticated = !!localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router
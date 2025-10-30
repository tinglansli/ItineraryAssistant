import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Auth from '../views/Auth.vue'
import CreateTrip from '../views/CreateTrip.vue'
import TripDetail from '../views/TripDetail.vue'
import TripList from '../views/TripList.vue'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/auth',
    name: 'Auth',
    component: Auth
  },
  {
    path: '/create-trip',
    name: 'CreateTrip',
    component: CreateTrip,
    meta: { requiresAuth: true }
  },
  {
    path: '/trips',
    name: 'TripList',
    component: TripList,
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId',
    name: 'TripDetail',
    component: TripDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId/budget',
    name: 'BudgetAnalysis',
    component: () => import('../views/BudgetAnalysis.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/trip/:tripId/expense',
    name: 'ExpenseRecord',
    component: () => import('../views/ExpenseRecord.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
})

// 路由守卫 - 检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  if (requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    next('/auth')
  } else if (to.path === '/auth' && token) {
    // 已登录用户访问登录页，跳转到主页
    next('/home')
  } else {
    // 放行
    next()
  }
})

export default router

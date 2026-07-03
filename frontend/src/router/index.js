import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import CategoryView from '../views/CategoryView.vue'
import SearchView from '../views/SearchView.vue'
import PerformanceDetailView from '../views/PerformanceDetailView.vue'
import MovieDetailView from '../views/MovieDetailView.vue'
import LoginView from '../views/LoginView.vue'
import UserCenterView from '../views/UserCenterView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import ViewersView from '../views/ViewersView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'
import SessionSeatView from '../views/SessionSeatView.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/city', name: 'city', component: CategoryView, props: { cityMode: true } },
    { path: '/category/:code?', name: 'category', component: CategoryView },
    { path: '/search', name: 'search', component: SearchView },
    { path: '/performances/:id', alias: '/performance/:id', name: 'performance-detail', component: PerformanceDetailView },
    { path: '/performance/:id/purchase', name: 'performance-purchase', component: PlaceholderView, meta: { requiresAuth: true, title: '购票选择', description: '请选择场次、票档和数量' } },
    { path: '/movies/:id', alias: '/movie/:id', name: 'movie-detail', component: MovieDetailView },
    { path: '/session/:id/seats', name: 'session-seats', component: SessionSeatView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/403', name: 'forbidden', component: ForbiddenView },
    { path: '/user', name: 'user-center', component: UserCenterView, meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: UserCenterView, meta: { requiresAuth: true } },
    { path: '/real-name', name: 'real-name', component: UserCenterView, meta: { requiresAuth: true, focus: 'realName' } },
    { path: '/viewers', name: 'viewers', component: ViewersView, meta: { requiresAuth: true } },
    { path: '/orders', name: 'orders', component: PlaceholderView, meta: { requiresAuth: true, title: '我的订单', description: '暂无订单' } },
    { path: '/tickets', name: 'tickets', component: PlaceholderView, meta: { requiresAuth: true, title: '我的票夹', description: '暂无电子票' } },
    { path: '/messages', name: 'messages', component: PlaceholderView, meta: { requiresAuth: true, title: '我的消息', description: '暂无消息' } },
    { path: '/admin/venue/:id/areas', name: 'admin-venue-areas', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdminEntry: true } },
    { path: '/admin/venue/:id/seats', name: 'admin-venue-seats', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdminEntry: true } },
    { path: '/admin/:section?', name: 'admin-dashboard', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdminEntry: true } }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  if (!user.initialized) {
    try {
      await user.fetchMe()
    } catch {
      // Expired sessions are handled by the HTTP layer.
    }
  }

  if (to.meta.requiresAuth && !user.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdminEntry) {
    if (!user.canEnterAdmin) {
      return { path: '/403', query: { from: to.fullPath } }
    }
    if (user.role === 'CHECKER' && to.params.section !== 'checkin') {
      return { path: '/admin/checkin' }
    }
  }

  if (to.path === '/login' && user.isLoggedIn) {
    return { path: '/user' }
  }

  return true
})

export default router

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
import PurchaseView from '../views/PurchaseView.vue'
import RushQueueView from '../views/RushQueueView.vue'
import RushResultView from '../views/RushResultView.vue'
import OrderConfirmView from '../views/OrderConfirmView.vue'
import PaymentView from '../views/PaymentView.vue'
import PaymentResultView from '../views/PaymentResultView.vue'
import OrdersView from '../views/OrdersView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import TicketsView from '../views/TicketsView.vue'
import TicketDetailView from '../views/TicketDetailView.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/city', name: 'city', component: CategoryView, props: { cityMode: true } },
    { path: '/category/:code?', name: 'category', component: CategoryView },
    { path: '/search', name: 'search', component: SearchView },
    { path: '/performances/:id', alias: '/performance/:id', name: 'performance-detail', component: PerformanceDetailView },
    { path: '/performance/:id/purchase', name: 'performance-purchase', component: PurchaseView, meta: { requiresAuth: true } },
    { path: '/movies/:id', alias: '/movie/:id', name: 'movie-detail', component: MovieDetailView },
    { path: '/session/:id/seats', name: 'session-seats', component: SessionSeatView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/403', name: 'forbidden', component: ForbiddenView },
    { path: '/user', name: 'user-center', component: UserCenterView, meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: UserCenterView, meta: { requiresAuth: true } },
    { path: '/real-name', name: 'real-name', component: UserCenterView, meta: { requiresAuth: true, focus: 'realName' } },
    { path: '/viewers', name: 'viewers', component: ViewersView, meta: { requiresAuth: true } },
    { path: '/rush/queue/:requestId', name: 'rush-queue', component: RushQueueView, meta: { requiresAuth: true } },
    { path: '/rush/result/:requestId', name: 'rush-result', component: RushResultView, meta: { requiresAuth: true } },
    { path: '/order/confirm/:orderId', name: 'order-confirm', component: OrderConfirmView, meta: { requiresAuth: true } },
    { path: '/payment/:orderId', name: 'payment', component: PaymentView, meta: { requiresAuth: true } },
    { path: '/payment/result/:orderId', name: 'payment-result', component: PaymentResultView, meta: { requiresAuth: true } },
    { path: '/ticket/success/:orderId', name: 'ticket-success', component: PaymentResultView, meta: { requiresAuth: true } },
    { path: '/orders', name: 'orders', component: OrdersView, meta: { requiresAuth: true } },
    { path: '/orders/:id', name: 'order-detail', component: OrderDetailView, meta: { requiresAuth: true } },
    { path: '/tickets', name: 'tickets', component: TicketsView, meta: { requiresAuth: true } },
    { path: '/tickets/:id', name: 'ticket-detail', component: TicketDetailView, meta: { requiresAuth: true } },
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

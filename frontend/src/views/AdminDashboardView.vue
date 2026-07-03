<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">T</span>
        <strong>星票后台</strong>
      </RouterLink>
      <nav>
        <RouterLink
          v-for="item in visibleMenus"
          :key="item.key"
          :to="`/admin/${item.key}`"
          :class="{ active: activeSection === item.key }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </RouterLink>
      </nav>
    </aside>
    <section class="admin-main">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>{{ activeMenu?.title || '运营概览' }}</h1>
        </div>
        <div class="admin-userbar">
          <span>{{ user.profile?.nickname || user.profile?.username }}</span>
          <el-tag effect="plain">{{ roleText }}</el-tag>
          <RouterLink to="/">返回前台</RouterLink>
          <el-button size="small" @click="logout">退出登录</el-button>
        </div>
      </header>

      <template v-if="activeSection === 'overview' || activeSection === undefined">
        <div class="metric-grid">
          <div class="metric"><span>演出</span><strong>{{ metrics.performanceCount }}</strong></div>
          <div class="metric"><span>电影</span><strong>{{ metrics.movieCount }}</strong></div>
          <div class="metric"><span>订单</span><strong>{{ metrics.orderCount }}</strong></div>
          <div class="metric"><span>电子票</span><strong>{{ metrics.ticketCount }}</strong></div>
          <div class="metric"><span>待审退票</span><strong>{{ metrics.refundPending }}</strong></div>
          <div class="metric"><span>今日核验</span><strong>{{ metrics.checkinToday }}</strong></div>
        </div>
        <section class="admin-card">
          <div class="table-head">
            <h2>运营待办</h2>
            <el-button type="primary" plain>刷新</el-button>
          </div>
          <el-table :data="todoRows" border>
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column prop="title" label="事项" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </section>
      </template>

      <template v-else-if="activeSection === 'checkin'">
        <div class="metric-grid">
          <div class="metric"><span>今日核验</span><strong>{{ checkerMetrics.checkinToday }}</strong></div>
          <div class="metric"><span>通过率</span><strong>{{ checkerMetrics.successRate }}</strong></div>
          <div class="metric wide"><span>通道状态</span><strong>{{ checkerMetrics.latestResult }}</strong></div>
        </div>
        <section class="admin-card empty-admin">
          <el-icon><Tickets /></el-icon>
          <h2>检票管理</h2>
          <p>请在检票入口输入票号或扫描二维码完成核验。</p>
        </section>
      </template>

      <section v-else class="admin-card empty-admin">
        <el-icon><FolderOpened /></el-icon>
        <h2>{{ activeMenu?.title }}</h2>
        <p>暂无数据</p>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../api/http'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const metrics = reactive({
  performanceCount: 0,
  movieCount: 0,
  orderCount: 0,
  ticketCount: 0,
  refundPending: 0,
  checkinToday: 0
})

const checkerMetrics = reactive({
  checkinToday: 0,
  successRate: '0%',
  latestResult: '暂无数据'
})

const roleMap = {
  ADMIN: '系统管理员',
  MANAGER: '票务管理员',
  CHECKER: '检票员'
}

const menus = [
  { key: 'overview', title: '运营概览', icon: 'DataAnalysis', roles: ['ADMIN', 'MANAGER'] },
  { key: 'users', title: '用户管理', icon: 'User', roles: ['ADMIN'] },
  { key: 'roles', title: '角色权限', icon: 'Lock', roles: ['ADMIN'] },
  { key: 'taxonomy', title: '城市与分类', icon: 'Collection', roles: ['ADMIN', 'MANAGER'] },
  { key: 'banners', title: '首页轮播', icon: 'Picture', roles: ['ADMIN', 'MANAGER'] },
  { key: 'sections', title: '首页推荐', icon: 'Star', roles: ['ADMIN', 'MANAGER'] },
  { key: 'performances', title: '演出管理', icon: 'Tickets', roles: ['ADMIN', 'MANAGER'] },
  { key: 'movies', title: '电影管理', icon: 'VideoCamera', roles: ['ADMIN', 'MANAGER'] },
  { key: 'venues', title: '场馆/座位', icon: 'Location', roles: ['ADMIN', 'MANAGER'] },
  { key: 'sessions', title: '场次/票档', icon: 'Calendar', roles: ['ADMIN', 'MANAGER'] },
  { key: 'batches', title: '售票批次', icon: 'Clock', roles: ['ADMIN', 'MANAGER'] },
  { key: 'stock', title: '库存池', icon: 'Box', roles: ['ADMIN', 'MANAGER'] },
  { key: 'orders', title: '订单管理', icon: 'Document', roles: ['ADMIN', 'MANAGER'] },
  { key: 'refunds', title: '退票审核', icon: 'RefreshLeft', roles: ['ADMIN', 'MANAGER'] },
  { key: 'checkin', title: '检票管理', icon: 'Checked', roles: ['ADMIN', 'MANAGER', 'CHECKER'] },
  { key: 'reports', title: '统计报表', icon: 'TrendCharts', roles: ['ADMIN', 'MANAGER'] },
  { key: 'operation-logs', title: '操作日志', icon: 'Notebook', roles: ['ADMIN'] },
  { key: 'risk-logs', title: '风控日志', icon: 'Warning', roles: ['ADMIN'] },
  { key: 'messages', title: '系统消息', icon: 'Message', roles: ['ADMIN', 'MANAGER'] }
]

const todoRows = [
  { type: '退票', title: '待审核退票申请', status: '待处理' },
  { type: '售票', title: '即将开售批次', status: '待关注' },
  { type: '核验', title: '今日入场核验', status: '进行中' }
]

const activeSection = computed(() => route.params.section || 'overview')
const visibleMenus = computed(() => menus.filter((item) => item.roles.includes(user.role)))
const activeMenu = computed(() => menus.find((item) => item.key === activeSection.value))
const roleText = computed(() => roleMap[user.role] || user.role)

const loadDashboard = async () => {
  if (user.canUseAdminApi) {
    Object.assign(metrics, await http.get('/api/admin/dashboard'))
  }
  if (user.canUseChecker) {
    Object.assign(checkerMetrics, await http.get('/api/checker/dashboard'))
  }
}

const logout = async () => {
  await user.logout()
  router.push('/')
}

onMounted(loadDashboard)
</script>

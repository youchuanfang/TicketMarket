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
          :to="item.path"
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
          <p class="eyebrow">星票后台管理</p>
          <h1>{{ activeMenu?.title || '运营概览' }}</h1>
        </div>
        <div class="admin-userbar">
          <span>{{ user.profile?.nickname || user.profile?.username }}</span>
          <el-tag effect="plain">{{ roleText }}</el-tag>
          <RouterLink to="/">返回前台</RouterLink>
          <el-button size="small" @click="logout">退出登录</el-button>
        </div>
      </header>

      <template v-if="activeSection === 'overview'">
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
            <el-button type="primary" plain @click="loadAll">刷新</el-button>
          </div>
          <el-table :data="todoRows" border empty-text="暂无数据">
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column prop="title" label="事项" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </section>
      </template>

      <section v-else-if="activeSection === 'venue'" class="admin-card">
        <div class="table-head">
          <h2>场馆管理</h2>
          <el-button type="primary" @click="createVenue">新增场馆</el-button>
        </div>
        <el-table :data="venues" border empty-text="暂无数据">
          <el-table-column prop="name" label="场馆" min-width="150" />
          <el-table-column prop="cityName" label="城市" width="100" />
          <el-table-column prop="address" label="地址" min-width="220" />
          <el-table-column prop="capacity" label="容量" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/areas`">区域</RouterLink>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/seats`">座位图</RouterLink>
              <el-button link type="danger" @click="disableVenue(row)">禁用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'venue-areas'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '场馆' }}区域</h2>
          <el-button type="primary" @click="createArea">新增区域</el-button>
        </div>
        <el-table :data="areas" border empty-text="暂无数据">
          <el-table-column prop="areaName" label="区域" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ areaTypeText(row.areaType) }}</template>
          </el-table-column>
          <el-table-column prop="defaultTicketLevel" label="默认票档" width="140" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="颜色" width="100">
            <template #default="{ row }"><span class="color-swatch" :style="{ background: row.color }" /></template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'seat-template'" class="admin-card">
        <div class="table-head">
          <h2>座位模板生成器</h2>
          <el-button type="primary" @click="generateSeatTemplate">生成座位</el-button>
        </div>
        <div class="resource-form">
          <el-select v-model="seatForm.venueId" placeholder="选择场馆" @change="loadAreasForSeatForm">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
          <el-select v-model="seatForm.areaId" placeholder="选择区域">
            <el-option v-for="area in seatFormAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
          <el-input-number v-model="seatForm.rowStart" :min="1" placeholder="起始排" />
          <el-input-number v-model="seatForm.rowEnd" :min="1" placeholder="结束排" />
          <el-input-number v-model="seatForm.seatsPerRow" :min="1" placeholder="每排座位" />
          <el-input v-model="seatForm.aisleAfterSeats" placeholder="过道位置，如 8,16" />
        </div>
        <SeatSvg :seats="previewSeats" selectable />
      </section>

      <section v-else-if="activeSection === 'venue-seats'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '场馆' }}座位图</h2>
          <el-tag>点击座位可查看状态</el-tag>
        </div>
        <SeatSvg :seats="venueSeats" selectable @seat-click="showSeat" />
      </section>

      <section v-else-if="activeSection === 'session'" class="admin-card">
        <div class="table-head">
          <h2>场次管理</h2>
          <el-button type="primary" @click="createSession">新增场次</el-button>
        </div>
        <el-table :data="sessions" border empty-text="暂无数据">
          <el-table-column prop="sessionName" label="场次" min-width="180" />
          <el-table-column label="购票模式" width="150">
            <template #default="{ row }">{{ purchaseModeText(row.purchaseMode) }}</template>
          </el-table-column>
          <el-table-column prop="saleStartTime" label="开售时间" width="180" />
          <el-table-column prop="lockTime" label="锁票时间" width="180" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="initSessionSeats(row)">初始化座位</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'ticket-level'" class="admin-card">
        <div class="table-head">
          <h2>票档管理</h2>
          <el-select v-model="selectedSessionId" placeholder="选择场次" @change="loadTicketLevels">
            <el-option v-for="session in sessions" :key="session.id" :label="session.sessionName" :value="session.id" />
          </el-select>
        </div>
        <el-table :data="ticketLevels" border empty-text="暂无数据">
          <el-table-column prop="name" label="票档" />
          <el-table-column prop="price" label="价格" width="120" />
          <el-table-column prop="totalStock" label="总库存" width="100" />
          <el-table-column prop="releasedStock" label="已开放" width="100" />
          <el-table-column prop="unreleasedStock" label="未开放" width="100" />
          <el-table-column prop="soldStock" label="已售" width="90" />
          <el-table-column prop="lockedStock" label="锁定" width="90" />
        </el-table>
      </section>

      <section v-else-if="activeSection === 'sale-batch'" class="admin-card">
        <div class="table-head">
          <h2>售票批次管理</h2>
          <el-button type="primary" @click="createBatch">创建批次</el-button>
        </div>
        <el-table :data="saleBatches" border empty-text="暂无数据">
          <el-table-column prop="batchName" label="批次" min-width="160" />
          <el-table-column prop="saleStartTime" label="开售时间" width="180" />
          <el-table-column prop="lockTime" label="锁票时间" width="180" />
          <el-table-column label="开放方式" width="110">
            <template #default="{ row }">{{ releaseTypeText(row.releaseType) }}</template>
          </el-table-column>
          <el-table-column prop="releaseQuantity" label="开放数量" width="110" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="startBatch(row)">开售</el-button>
              <el-button link type="warning" @click="lockBatch(row)">锁票</el-button>
              <el-button link type="success" @click="initRedis(row)">初始化库存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'stock-pool'" class="admin-card">
        <div class="table-head">
          <h2>库存池</h2>
          <el-tag type="info">锁票后回收和退票待释放库存</el-tag>
        </div>
        <el-table :data="stockPool" border empty-text="暂无数据">
          <el-table-column prop="sessionId" label="场次" width="100" />
          <el-table-column prop="ticketLevelId" label="票档" width="100" />
          <el-table-column label="来源">
            <template #default="{ row }">{{ sourceTypeText(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="库存状态">
            <template #default="{ row }">{{ statusText(row.stockStatus) }}</template>
          </el-table-column>
          <el-table-column label="可用于下一批" width="140">
            <template #default="{ row }">{{ row.availableForNextBatch ? '可用' : '不可用' }}</template>
          </el-table-column>
        </el-table>
      </section>

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
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../api/http'
import { adminApi } from '../api/adminResources'
import { useUserStore } from '../stores/user'

const SeatSvg = {
  props: { seats: { type: Array, default: () => [] }, selectable: Boolean },
  emits: ['seat-click'],
  setup(props, { emit }) {
    const color = (status) => ({
      AVAILABLE: '#2f9e44',
      LOCKED: '#f59f00',
      SOLD: '#868e96',
      DISABLED: '#ced4da',
      UNRELEASED: '#adb5bd',
      POST_LOCK_RETURNED: '#7048e8',
      REFUND_WAITING_RELEASE: '#0b7285',
      CHECKED_IN: '#495057'
    }[status] || '#2f9e44')
    return () => h('div', { class: 'seat-map-wrap' }, [
      h('div', { class: 'stage-line' }, '舞台 / 银幕'),
      h('svg', { viewBox: '0 0 760 560', class: 'seat-map' },
        props.seats.map((seat) => h('circle', {
          cx: seat.x,
          cy: seat.y,
          r: 8,
          fill: color(seat.status),
          class: props.selectable ? 'seat-dot clickable' : 'seat-dot',
          onClick: () => emit('seat-click', seat)
        }, [h('title', seat.seatLabel || `${seat.rowNo}排${seat.seatNo}座`)]))
      ),
      h('div', { class: 'seat-legend' }, [
        ['AVAILABLE', '可售'], ['LOCKED', '锁定'], ['SOLD', '已售'], ['DISABLED', '不可售'], ['UNRELEASED', '未开放'], ['POST_LOCK_RETURNED', '锁票后回收']
      ].map(([status, label]) => h('span', [h('i', { style: { background: color(status) } }), label])))
    ])
  }
}

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const metrics = reactive({ performanceCount: 0, movieCount: 0, orderCount: 0, ticketCount: 0, refundPending: 0, checkinToday: 0 })
const checkerMetrics = reactive({ checkinToday: 0, successRate: '0%', latestResult: '暂无数据' })
const venues = ref([])
const areas = ref([])
const venueSeats = ref([])
const previewSeats = ref([])
const sessions = ref([])
const ticketLevels = ref([])
const saleBatches = ref([])
const stockPool = ref([])
const selectedSessionId = ref(null)
const seatFormAreas = ref([])
const seatForm = reactive({ venueId: 1, areaId: 1, rowStart: 1, rowEnd: 3, seatsPerRow: 12, startX: 60, startY: 80, gapX: 30, gapY: 30, aisleAfterSeats: '6' })

const roleMap = { ADMIN: '系统管理员', MANAGER: '票务管理员', CHECKER: '检票员' }
const menus = [
  { key: 'overview', title: '运营概览', icon: 'DataAnalysis', path: '/admin', roles: ['ADMIN', 'MANAGER'] },
  { key: 'venue', title: '场馆管理', icon: 'Location', path: '/admin/venue', roles: ['ADMIN', 'MANAGER'] },
  { key: 'seat-template', title: '座位模板', icon: 'Grid', path: '/admin/seat-template', roles: ['ADMIN', 'MANAGER'] },
  { key: 'session', title: '场次管理', icon: 'Calendar', path: '/admin/session', roles: ['ADMIN', 'MANAGER'] },
  { key: 'ticket-level', title: '票档管理', icon: 'Tickets', path: '/admin/ticket-level', roles: ['ADMIN', 'MANAGER'] },
  { key: 'sale-batch', title: '售票批次', icon: 'Clock', path: '/admin/sale-batch', roles: ['ADMIN', 'MANAGER'] },
  { key: 'stock-pool', title: '库存池', icon: 'Box', path: '/admin/stock-pool', roles: ['ADMIN', 'MANAGER'] },
  { key: 'orders', title: '订单管理', icon: 'Document', path: '/admin/orders', roles: ['ADMIN', 'MANAGER'] },
  { key: 'refunds', title: '退票审核', icon: 'RefreshLeft', path: '/admin/refunds', roles: ['ADMIN', 'MANAGER'] },
  { key: 'checkin', title: '检票管理', icon: 'Checked', path: '/admin/checkin', roles: ['ADMIN', 'MANAGER', 'CHECKER'] },
  { key: 'reports', title: '统计报表', icon: 'TrendCharts', path: '/admin/reports', roles: ['ADMIN', 'MANAGER'] },
  { key: 'users', title: '用户管理', icon: 'User', path: '/admin/users', roles: ['ADMIN'] },
  { key: 'roles', title: '角色权限', icon: 'Lock', path: '/admin/roles', roles: ['ADMIN'] },
  { key: 'risk-logs', title: '风控日志', icon: 'Warning', path: '/admin/risk-logs', roles: ['ADMIN'] }
]
const todoRows = [
  { type: '退票', title: '待审核退票申请', status: '待处理' },
  { type: '售票', title: '即将开售批次', status: '待关注' },
  { type: '核验', title: '今日入场核验', status: '进行中' }
]
const statusMap = {
  ENABLED: '启用',
  DISABLED: '禁用',
  SCHEDULED: '待开售',
  NOT_STARTED: '未开售',
  SELLING: '售票中',
  LOCKED: '已锁票',
  FINISHED: '已结束',
  AVAILABLE: '可售',
  UNRELEASED: '未开放',
  SOLD: '已售',
  POST_LOCK_RETURNED: '锁票回收',
  REFUND_WAITING_RELEASE: '退票待释放',
  WAITING_RELEASE: '待释放',
  CHECKED_IN: '已核验'
}
const purchaseModeMap = {
  SELECTABLE: '自主选座',
  AUTO_ALLOCATE: '系统配座'
}
const releaseTypeMap = {
  FULL: '全部开放',
  PARTIAL: '分批开放',
  MANUAL: '手动开放',
  QUANTITY: '按数量开放',
  RATIO: '按比例开放'
}
const areaTypeMap = {
  SEATED: '有座区域',
  STANDING: '站席区域'
}
const sourceTypeMap = {
  POST_LOCK_RETURNED: '锁票回收',
  REFUND_WAITING_RELEASE: '退票待释放',
  UNRELEASED: '未开放库存',
  MANUAL_ADD: '人工调整'
}
const textFromMap = (map, value) => map[value] || value || '暂无数据'
const statusText = (value) => textFromMap(statusMap, value)
const purchaseModeText = (value) => textFromMap(purchaseModeMap, value)
const releaseTypeText = (value) => textFromMap(releaseTypeMap, value)
const areaTypeText = (value) => textFromMap(areaTypeMap, value)
const sourceTypeText = (value) => textFromMap(sourceTypeMap, value)

const activeSection = computed(() => {
  if (route.name === 'admin-venue-areas') return 'venue-areas'
  if (route.name === 'admin-venue-seats') return 'venue-seats'
  return route.params.section || 'overview'
})
const visibleMenus = computed(() => menus.filter((item) => item.roles.includes(user.role)))
const activeMenu = computed(() => menus.find((item) => item.key === activeSection.value))
const roleText = computed(() => roleMap[user.role] || user.role)
const selectedVenue = computed(() => venues.value.find((item) => String(item.id) === String(route.params.id || seatForm.venueId)))

const loadAll = async () => {
  if (user.canUseAdminApi) {
    Object.assign(metrics, await adminApi.dashboard())
    venues.value = await adminApi.venues()
    sessions.value = await adminApi.sessions()
    saleBatches.value = await adminApi.saleBatches()
    stockPool.value = await adminApi.stockPool()
    if (!selectedSessionId.value && sessions.value.length) selectedSessionId.value = sessions.value[0].id
    await loadTicketLevels()
    await loadAreasForRoute()
    await loadVenueSeatsForRoute()
    await loadAreasForSeatForm()
  }
  if (user.canUseChecker) Object.assign(checkerMetrics, await http.get('/api/checker/dashboard'))
}
const loadAreasForRoute = async () => {
  if (route.params.id) areas.value = await adminApi.areas(route.params.id)
}
const loadVenueSeatsForRoute = async () => {
  if (route.params.id) venueSeats.value = await adminApi.seats(route.params.id)
}
const loadAreasForSeatForm = async () => {
  if (seatForm.venueId) seatFormAreas.value = await adminApi.areas(seatForm.venueId)
  if (!seatForm.areaId && seatFormAreas.value.length) seatForm.areaId = seatFormAreas.value[0].id
}
const loadTicketLevels = async () => {
  if (selectedSessionId.value) ticketLevels.value = await adminApi.ticketLevels(selectedSessionId.value)
}

const createVenue = async () => {
  await adminApi.createVenue({ name: '新场馆', cityName: '上海', address: '请补充详细地址', capacity: 100, description: '标准票务场馆' })
  ElMessage.success('场馆已新增')
  await loadAll()
}
const disableVenue = async (row) => {
  await adminApi.deleteVenue(row.id)
  ElMessage.success('场馆已禁用')
  await loadAll()
}
const createArea = async () => {
  await adminApi.createArea(route.params.id, { areaName: '新增区域', areaType: 'SEATED', defaultTicketLevel: '标准票', color: '#d9303e' })
  ElMessage.success('区域已新增')
  await loadAreasForRoute()
}
const generateSeatTemplate = async () => {
  previewSeats.value = await adminApi.generateSeats(seatForm.venueId, seatForm)
  ElMessage.success(`已生成 ${previewSeats.value.length} 个座位`)
}
const showSeat = (seat) => ElMessage.info(`${seat.seatLabel}：${statusText(seat.status)}`)
const createSession = async () => {
  await adminApi.createSession({ sessionName: '新增场次', purchaseMode: 'SELECTABLE' })
  ElMessage.success('场次已新增')
  await loadAll()
}
const initSessionSeats = async (row) => {
  await adminApi.initSessionSeats(row.id)
  ElMessage.success('场次座位已初始化')
}
const createBatch = async () => {
  await adminApi.createSaleBatch({ sessionId: selectedSessionId.value || 1001, batchName: '新增售票批次', releaseQuantity: 50 })
  ElMessage.success('售票批次已创建')
  await loadAll()
}
const startBatch = async (row) => {
  await adminApi.startBatch(row.id)
  ElMessage.success('售票批次已开售')
  await loadAll()
}
const lockBatch = async (row) => {
  await adminApi.lockBatch(row.id)
  ElMessage.success('售票批次已锁票')
  await loadAll()
}
const initRedis = async (row) => {
  await adminApi.initRedisStock(row.id)
  ElMessage.success('实时库存已初始化')
}
const logout = async () => {
  await user.logout()
  router.push('/')
}

watch(() => route.fullPath, loadAll)
onMounted(loadAll)
</script>

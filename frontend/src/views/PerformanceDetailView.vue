<template>
  <div class="page detail-page" v-if="detail">
    <section class="detail-hero">
      <img :src="detail.poster" :alt="detail.title" class="detail-poster" />
      <div class="detail-main">
        <p class="eyebrow">{{ detail.categoryName }} · {{ statusText }}</p>
        <h1>{{ detail.title }}</h1>
        <p class="summary">{{ detail.summary }}</p>
        <div class="detail-meta">
          <span><el-icon><Location /></el-icon>{{ detail.city }} · {{ detail.venue }}</span>
          <span><el-icon><Calendar /></el-icon>{{ detail.startTime }}</span>
          <span><el-icon><Money /></el-icon>¥{{ detail.priceMin }} - ¥{{ detail.priceMax }}</span>
        </div>
        <div class="tag-row">
          <el-tag v-for="tag in detail.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
        </div>
        <div class="sale-timers">
          <div class="status-panel">
            <strong>{{ selectedSaleStatus.buttonText || '请选择场次' }}</strong>
            <span>{{ saleStatusDescription }}</span>
          </div>
        </div>
      </div>
      <aside class="buy-panel">
        <h3>购票模式</h3>
        <strong>{{ selectedSession ? modeLabel(selectedSession.purchaseMode) : modeText }}</strong>
        <p>{{ selectedSession?.purchaseMode === 'SELECTABLE' ? '该场次开售后支持自主选座。' : '请选择票档和数量后继续。' }}</p>
        <el-button type="primary" size="large" :disabled="buyDisabled" @click="buyNow">{{ actionText }}</el-button>
      </aside>
    </section>

    <section class="detail-section">
      <SectionHeader title="场次选择" />
      <div class="session-grid">
        <button
          v-for="session in sessions"
          :key="session.id"
          :class="['session-card', { active: selectedSession?.id === session.id }]"
          @click="selectSession(session)"
        >
          <strong>{{ session.sessionName }}</strong>
          <span>{{ session.startTime }} · {{ modeLabel(session.purchaseMode) }}</span>
        </button>
      </div>
    </section>

    <section class="detail-section">
      <SectionHeader title="票档展示" />
      <div class="ticket-levels">
        <div v-for="level in ticketLevels" :key="level.id" class="ticket-level">
          <span>{{ level.name }}</span>
          <strong>¥{{ level.price }}</strong>
          <em>{{ level.frontStatus || '可选' }}</em>
        </div>
      </div>
    </section>

    <section class="info-tabs">
      <el-tabs>
        <el-tab-pane label="演出介绍"><p>{{ detail.intro }}</p></el-tab-pane>
        <el-tab-pane label="艺人/演职人员"><p>{{ detail.artistInfo }}</p></el-tab-pane>
        <el-tab-pane label="场馆介绍"><p>{{ detail.venueIntro }}</p></el-tab-pane>
        <el-tab-pane label="购票须知"><p>{{ detail.purchaseNotice }}</p></el-tab-pane>
        <el-tab-pane label="退票规则"><p>{{ detail.refundRule }}</p></el-tab-pane>
        <el-tab-pane label="入场规则"><p>{{ detail.entryRule }}</p></el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { getPerformance, getPerformanceSessions, getSessionSaleStatus, getSessionTicketLevels } from '../api/portal'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const sessions = ref([])
const ticketLevels = ref([])
const selectedSession = ref(null)
const selectedSaleStatus = ref({})

const statusMap = {
  ON_SALE: '正在售票',
  COMING_SOON: '即将开售',
  RETURNED: '正在售卖',
  LOCKED: '已结束'
}
const modeMap = {
  SELECTABLE: '支持自主选座',
  AUTO_ALLOCATE: '自动分配座位',
  AREA_ONLY: '只选票档/区域',
  STANDING: '站席'
}

const statusText = computed(() => statusMap[detail.value?.saleStatus] || '')
const modeText = computed(() => modeMap[detail.value?.saleMode] || '')
const modeLabel = (mode) => modeMap[mode] || mode
const actionText = computed(() => selectedSaleStatus.value.buttonText || '请选择场次')
const buyDisabled = computed(() => !selectedSaleStatus.value.clickable)
const saleStatusDescription = computed(() => {
  const status = selectedSaleStatus.value.status
  if (status === 'RESERVABLE') return `开售时间：${selectedSaleStatus.value.saleStartTime || selectedSession.value?.saleStartTime}`
  if (status === 'ON_SALE') return '当前场次正在售卖，请按需选择票档和观演人。'
  if (status === 'SOLD_OUT') return '当前票档紧张，可稍后再查看。'
  if (status === 'ENDED') return '当前场次售卖已结束。'
  return '请选择场次查看售卖状态。'
})

const selectSession = async (session) => {
  selectedSession.value = session
  const [levels, saleStatus] = await Promise.all([
    getSessionTicketLevels(session.id),
    getSessionSaleStatus(session.id)
  ])
  ticketLevels.value = levels
  selectedSaleStatus.value = saleStatus
}

const buyNow = () => {
  if (!selectedSession.value) {
    ElMessage.warning('请先选择场次')
    return
  }
  if (selectedSaleStatus.value.status === 'RESERVABLE') {
    router.push(`/performance/${detail.value.id}/purchase?mode=reservation&sessionId=${selectedSession.value.id}`)
  } else if (selectedSession.value.purchaseMode === 'SELECTABLE') {
    router.push(`/session/${selectedSession.value.id}/seats`)
  } else {
    router.push(`/performance/${detail.value.id}/purchase?sessionId=${selectedSession.value.id}`)
  }
}

onMounted(async () => {
  try {
    detail.value = await getPerformance(route.params.id)
    sessions.value = await getPerformanceSessions(route.params.id)
    if (sessions.value.length) {
      await selectSession(sessions.value[0])
    } else {
      ticketLevels.value = detail.value.ticketLevels || []
    }
  } catch (error) {
    ElMessage.error(error.message)
    router.push('/')
  }
})
</script>

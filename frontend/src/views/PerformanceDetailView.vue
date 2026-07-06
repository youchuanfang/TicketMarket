<template>
  <div class="page detail-page" v-if="detail">
    <section class="detail-hero">
      <img :src="assetUrl(detail.poster)" :alt="detail.title" class="detail-poster" />
      <div class="detail-main">
        <p class="eyebrow">{{ detail.categoryName }} · {{ statusText }}</p>
        <h1>{{ detail.title }}</h1>
        <p class="summary">{{ detail.subtitle || detail.summary }}</p>
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
      <p v-if="!sessions.length" class="empty-inline">暂无可选场次，请在后台发布页保存后生成场次。</p>
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
      <p v-if="!ticketLevels.length" class="empty-inline">请选择场次后查看票档。</p>
    </section>

    <section class="info-tabs">
      <el-tabs>
        <el-tab-pane label="项目详情">
          <div class="rich-detail">
            <div v-if="detail.detailContent" class="rich-detail-content" v-html="detailHtml"></div>
            <template v-else-if="detailBlocks.length">
              <template v-for="(block, index) in detailBlocks" :key="index">
                <h2 v-if="block.type === 'HEADING'">{{ block.content }}</h2>
                <img v-else-if="block.type === 'IMAGE'" :src="assetUrl(block.content)" :alt="block.alt || detail.title" />
                <p v-else>{{ block.content }}</p>
              </template>
            </template>
            <template v-else>
              <img v-if="detail.detailImage" :src="assetUrl(detail.detailImage)" :alt="detail.title" />
              <h2>项目介绍</h2>
              <p>{{ detail.intro }}</p>
              <h2>演职人员</h2>
              <p>{{ detail.artistInfo }}</p>
              <h2>场馆介绍</h2>
              <p>{{ detail.venueIntro }}</p>
            </template>
          </div>
        </el-tab-pane>
        <el-tab-pane label="购票须知">
          <p>{{ detail.purchaseNotice }}</p>
          <table v-if="refundRows.length" class="refund-table">
            <thead>
              <tr>
                <th>申请退票时间段</th>
                <th>退票手续费</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in refundRows" :key="row.period">
                <td>{{ row.period }}</td>
                <td>{{ row.fee }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else>{{ detail.refundRule }}</p>
        </el-tab-pane>
        <el-tab-pane label="观演须知"><p>{{ detail.entryRule }}</p></el-tab-pane>
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
import { assetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const sessions = ref([])
const ticketLevels = ref([])
const selectedSession = ref(null)
const selectedSaleStatus = ref({})

const statusMap = {
  ON_SALE: '热卖中',
  COMING_SOON: '即将开售',
  SOLD_OUT: '已售罄',
  ENDED: '已结束',
  RETURNED: '热卖中',
  LOCKED: '已结束'
}
const modeMap = {
  SELECTABLE: '支持自主选座',
  AUTO_ALLOCATE: '自动分配座位',
  AREA_ONLY: '只选票档/区域',
  STANDING: '站席'
}

const statusText = computed(() => statusMap[selectedSaleStatus.value.status] || statusMap[detail.value?.saleStatus] || '')
const modeText = computed(() => modeMap[detail.value?.saleMode] || '')
const detailBlocks = computed(() => detail.value?.detailBlocks || [])
const detailHtml = computed(() => rewriteImageSources(detail.value?.detailContent || ''))
const modeLabel = (mode) => modeMap[mode] || mode
const refundRows = computed(() => {
  if (!detail.value?.refundFreeUntil || !detail.value?.refundFeeUntil || !detail.value?.refundStopTime) return []
  const saleStart = selectedSaleStatus.value.saleStartTime || selectedSession.value?.saleStartTime || detail.value.startTime
  return [
    { period: `${saleStart} 至 ${detail.value.refundFreeUntil}`, fee: '无手续费' },
    { period: `${detail.value.refundFeeUntil} 至 ${detail.value.refundStopTime}`, fee: '票价20%' },
    { period: `${detail.value.refundStopTime} 之后`, fee: '停止退票' }
  ]
})
const actionText = computed(() => selectedSaleStatus.value.buttonText || '请选择场次')
const buyDisabled = computed(() => !selectedSaleStatus.value.clickable)
const saleStatusDescription = computed(() => {
  const status = selectedSaleStatus.value.status
  if (status === 'COMING_SOON') return `开售时间：${selectedSaleStatus.value.saleStartTime || selectedSession.value?.saleStartTime}`
  if (status === 'ON_SALE') return '当前场次热卖中，可直接购票，也可以沿用最近一次预约信息。'
  if (status === 'SOLD_OUT') return '当前场次已售罄，如有退票回流且未锁票会自动恢复热卖。'
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
  if (selectedSaleStatus.value.status === 'COMING_SOON') {
    router.push(`/performance/${detail.value.id}/purchase?mode=reservation&sessionId=${selectedSession.value.id}`)
  } else if (selectedSession.value.purchaseMode === 'SELECTABLE') {
    router.push(`/session/${selectedSession.value.id}/seats`)
  } else {
    router.push(`/performance/${detail.value.id}/purchase?sessionId=${selectedSession.value.id}`)
  }
}

const rewriteImageSources = (html) => String(html || '').replace(/src="([^"]+)"/g, (_, src) => `src="${assetUrl(src)}"`)

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

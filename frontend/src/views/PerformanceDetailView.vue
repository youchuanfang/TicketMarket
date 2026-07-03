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
          <StatusPanel icon="Clock" title="开售提醒" :description="selectedSession ? `开售时间：${selectedSession.saleStartTime}` : '请选择场次查看开售时间'" />
          <StatusPanel icon="Lock" title="锁票提醒" :description="selectedSession ? `锁票时间：${selectedSession.lockTime}` : '锁票后本轮将停止售卖'" type="warning" />
        </div>
      </div>
      <aside class="buy-panel">
        <h3>购票模式</h3>
        <strong>{{ selectedSession ? modeLabel(selectedSession.purchaseMode) : modeText }}</strong>
        <p>{{ selectedSession?.purchaseMode === 'SELECTABLE' ? '该场次支持自主选座。' : '请选择票档和数量后进入购票流程。' }}</p>
        <el-button type="primary" size="large" @click="buyNow">{{ selectedSession?.purchaseMode === 'SELECTABLE' ? '选座购票' : '立即购票' }}</el-button>
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
          <em>已开放 {{ level.releasedStock }}</em>
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
import StatusPanel from '../components/StatusPanel.vue'
import { getPerformance, getPerformanceSessions, getSessionTicketLevels } from '../api/portal'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const sessions = ref([])
const ticketLevels = ref([])
const selectedSession = ref(null)

const statusMap = {
  ON_SALE: '正在售票',
  COMING_SOON: '即将开售',
  RETURNED: '少量回流',
  LOCKED: '本轮锁票'
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

const selectSession = async (session) => {
  selectedSession.value = session
  ticketLevels.value = await getSessionTicketLevels(session.id)
}

const buyNow = () => {
  if (!selectedSession.value) {
    ElMessage.warning('请先选择场次')
    return
  }
  if (selectedSession.value.purchaseMode === 'SELECTABLE') {
    router.push(`/session/${selectedSession.value.id}/seats`)
  } else {
    router.push(`/performance/${detail.value.id}/purchase`)
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

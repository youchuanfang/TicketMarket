<template>
  <div class="page narrow">
    <SectionHeader title="购票选择" eyebrow="确认场次与票档" />
    <section class="flow-panel">
      <div class="flow-main">
        <h2>{{ performance?.title || '活动详情' }}</h2>
        <el-form label-position="top">
          <el-form-item label="场次">
            <el-select v-model="form.sessionId" placeholder="请选择场次" @change="loadLevels">
              <el-option
                v-for="session in sessions"
                :key="session.id"
                :label="`${session.sessionName} ${sessionStatusLabel(session)}`"
                :value="session.id"
                :disabled="isSessionDisabled(session)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="票档">
            <el-select v-model="form.ticketLevelId" placeholder="请选择票档">
              <el-option
                v-for="level in ticketLevels"
                :key="level.id"
                :label="`${level.name} ¥${level.price}${level.availableStock <= 0 ? '（售罄）' : ''}`"
                :value="level.id"
                :disabled="saleStatus.status === 'ON_SALE' && level.availableStock <= 0"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="form.quantity" :min="1" :max="2" />
          </el-form-item>
          <el-form-item label="观演人">
            <el-select v-model="form.viewerIds" multiple placeholder="请选择观演人">
              <el-option v-for="viewer in viewers" :key="viewer.id" :label="`${viewer.name} ${viewer.idCardMasked}`" :value="viewer.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <aside class="flow-aside">
        <p>购票模式</p>
        <strong>{{ modeText }}</strong>
        <p>售卖状态</p>
        <strong>{{ saleStatusText }}</strong>
        <el-button type="primary" size="large" :disabled="submitDisabled" :loading="submitting" @click="submit">{{ submitText }}</el-button>
        <el-button v-if="selectedSession?.purchaseMode === 'SELECTABLE'" plain @click="goSeat">前往选座</el-button>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { getActiveBatch, getPerformance, getPerformanceSessions, getSessionSaleStatus, getSessionTicketLevels } from '../api/portal'
import { getViewers } from '../api/auth'
import { createReservation, getLatestReservation, submitRush } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const performance = ref(null)
const sessions = ref([])
const ticketLevels = ref([])
const viewers = ref([])
const batch = ref(null)
const saleStatus = ref({})
const submitting = ref(false)
const form = reactive({ sessionId: null, ticketLevelId: null, quantity: 1, viewerIds: [] })

const selectedSession = computed(() => sessions.value.find((item) => item.id === form.sessionId))
const modeText = computed(() => ({
  SELECTABLE: '支持自主选座',
  AUTO_ALLOCATE: '系统自动配座',
  AREA_ONLY: '按区域购票',
  STANDING: '站席购票'
}[selectedSession.value?.purchaseMode] || '请选择场次'))
const saleStatusText = computed(() => ({
  COMING_SOON: '即将开售，可先预约',
  ON_SALE: '热卖中',
  SOLD_OUT: '已售罄',
  ENDED: '已结束',
  UNAVAILABLE: '暂不可售'
}[saleStatus.value.status] || '请选择场次'))
const submitText = computed(() => saleStatus.value.status === 'ON_SALE' ? '立即购票' : '预约抢票')
const submitDisabled = computed(() => !form.sessionId || !form.ticketLevelId || ['SOLD_OUT', 'ENDED', 'UNAVAILABLE'].includes(saleStatus.value.status))
const isSessionDisabled = (session) => ['SOLD_OUT', 'ENDED', 'UNAVAILABLE'].includes(session.saleStatus)
const sessionStatusLabel = (session) => {
  const text = ({
    COMING_SOON: '即将开售',
    ON_SALE: '热卖中',
    SOLD_OUT: '已售罄',
    ENDED: '已结束',
    UNAVAILABLE: '暂不可售'
  })[session.saleStatus]
  return text ? `（${text}）` : ''
}

const loadLevels = async () => {
  if (!form.sessionId) return
  const [levels, status] = await Promise.all([
    getSessionTicketLevels(form.sessionId),
    getSessionSaleStatus(form.sessionId)
  ])
  ticketLevels.value = levels
  saleStatus.value = status
  form.ticketLevelId = ticketLevels.value.find((level) => saleStatus.value.status !== 'ON_SALE' || level.availableStock > 0)?.id || null
  batch.value = await getActiveBatch(form.sessionId)
}

const applyLatestReservation = async () => {
  if (route.query.mode === 'reservation') return
  const latest = await getLatestReservation({ performanceId: route.params.id })
  if (!latest?.sessionId) return
  form.sessionId = latest.sessionId
  await loadLevels()
  if (ticketLevels.value.some((level) => level.id === latest.ticketLevelId)) {
    form.ticketLevelId = latest.ticketLevelId
  }
  form.quantity = latest.quantity || 1
  form.viewerIds = Array.isArray(latest.viewerIds) ? latest.viewerIds : []
}

const submit = async () => {
  if (!form.sessionId || !form.ticketLevelId) {
    ElMessage.warning('请选择场次和票档')
    return
  }
  if (form.viewerIds.length !== form.quantity) {
    ElMessage.warning('请选择对应数量的观演人')
    return
  }
  submitting.value = true
  try {
    if (saleStatus.value.status === 'ON_SALE') {
      const request = await submitRush({ ...form, batchId: batch.value?.batchId || batch.value?.id })
      router.push(`/rush/queue/${request.requestId}`)
    } else {
      await createReservation({ ...form, batchId: batch.value?.batchId || batch.value?.id })
      ElMessage.success('预约抢票已提交')
      router.push(`/performances/${performance.value.id}`)
    }
  } finally {
    submitting.value = false
  }
}

const goSeat = () => {
  if (!form.sessionId) {
    ElMessage.warning('请先选择场次')
    return
  }
  if (saleStatus.value.status !== 'ON_SALE') {
    ElMessage.warning('当前场次暂未开放选座，可先预约抢票')
    return
  }
  router.push(`/session/${form.sessionId}/seats`)
}

onMounted(async () => {
  performance.value = await getPerformance(route.params.id)
  const sessionRows = await getPerformanceSessions(route.params.id)
  const statuses = await Promise.all(sessionRows.map((session) => getSessionSaleStatus(session.id)))
  sessions.value = sessionRows.map((session, index) => ({
    ...session,
    saleStatus: statuses[index]?.status,
    clickable: statuses[index]?.clickable
  }))
  viewers.value = await getViewers()
  const requestedSession = sessions.value.find((session) => session.id === Number(route.query.sessionId) && !isSessionDisabled(session))
  form.sessionId = requestedSession?.id || sessions.value.find((session) => !isSessionDisabled(session))?.id || null
  form.viewerIds = viewers.value.filter((item) => item.defaultViewer).map((item) => item.id)
  await loadLevels()
  await applyLatestReservation()
})
</script>

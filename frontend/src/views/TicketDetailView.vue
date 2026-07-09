<template>
  <div class="page narrow">
    <SectionHeader title="电子票详情" eyebrow="入场核验" />
    <section v-if="ticket" class="ticket-detail">
      <div class="ticket-detail-head">
        <img v-if="assetUrl(ticket.poster)" :src="assetUrl(ticket.poster)" :alt="ticket.itemTitle" class="ticket-detail-poster" />
        <div v-else class="ticket-detail-poster placeholder">暂无海报</div>
        <div>
          <h2>{{ ticket.itemTitle || '电子票' }}</h2>
          <strong>{{ statusText(ticket.status) }}</strong>
          <p>{{ ticket.sessionTime || ticket.sessionName || '场次待确认' }}</p>
          <p>{{ ticket.venueName }} {{ ticket.cityName ? `/ ${ticket.cityName}` : '' }}</p>
          <p>{{ ticket.ticketLevelName || '票档待确认' }}</p>
          <p>观演人：{{ ticket.viewerName || '无需观演人' }} <span v-if="ticket.viewerIdCardMasked">{{ ticket.viewerIdCardMasked }}</span></p>
        </div>
      </div>
      <div class="qr-box dynamic-code">{{ dynamicCode || (ticket.status === 'UNUSED' ? '正在生成动态入场码' : '当前票状态不可入场') }}</div>
      <div v-if="ticket.status === 'UNUSED'" class="ticket-code-meta">
        <span>动态入场码 {{ countdown }} 秒后刷新</span>
        <el-button type="primary" plain @click="loadDynamicCode">刷新动态码</el-button>
      </div>
      <p>票号：{{ ticket.ticketNo }}</p>
      <p>订单编号：{{ ticket.orderNo || ticket.orderId }}</p>
      <p class="muted-text">请在入场时出示本页动态码，并配合工作人员核对观演人实名信息。</p>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SectionHeader from '../components/SectionHeader.vue'
import { getTicket, getTicketDynamicCode } from '../api/ticketFlow'
import { assetUrl } from '../utils/assets'

const route = useRoute()
const ticket = ref(null)
const dynamicCode = ref('')
const countdown = ref(0)
let timer = null
const statusText = (status) => ({ UNUSED: '待入场', CHECKED_IN: '已入场', REFUNDED: '已退票', INVALID: '已失效', EXPIRED: '已过期' }[status] || status)

const loadDynamicCode = async () => {
  const result = await getTicketDynamicCode(route.params.id)
  ticket.value = { ...ticket.value, ...result }
  dynamicCode.value = result.dynamicCode
  countdown.value = result.dynamicCodeTtlSeconds || 60
}

const tick = async () => {
  countdown.value = Math.max(0, countdown.value - 1)
  if (countdown.value <= 0 && ticket.value?.status === 'UNUSED') {
    await loadDynamicCode()
  }
}

onMounted(async () => {
  ticket.value = await getTicket(route.params.id)
  if (ticket.value?.status === 'UNUSED') {
    await loadDynamicCode()
    timer = window.setInterval(tick, 1000)
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

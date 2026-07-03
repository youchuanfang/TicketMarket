<template>
  <div class="page narrow">
    <SectionHeader title="电子票详情" eyebrow="入场核验" />
    <section v-if="ticket" class="ticket-detail">
      <div class="qr-box">{{ ticket.ticketNo }}</div>
      <h2>{{ statusText(ticket.status) }}</h2>
      <p>入场码：{{ ticket.qrCodeContent }}</p>
      <p>订单编号：{{ ticket.orderId }}</p>
      <p>请在入场时向工作人员出示本页。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SectionHeader from '../components/SectionHeader.vue'
import { getTicket } from '../api/ticketFlow'

const route = useRoute()
const ticket = ref(null)
const statusText = (status) => ({ UNUSED: '待入场', CHECKED_IN: '已入场', REFUNDED: '已退票', INVALID: '已失效', EXPIRED: '已过期' }[status] || status)

onMounted(async () => {
  ticket.value = await getTicket(route.params.id)
})
</script>

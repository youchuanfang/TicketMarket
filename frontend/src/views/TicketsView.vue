<template>
  <div class="page narrow">
    <SectionHeader title="我的票夹" eyebrow="电子票" />
    <section class="ticket-grid" v-if="tickets.length">
      <RouterLink v-for="ticket in tickets" :key="ticket.id" class="ticket-card" :to="`/tickets/${ticket.id}`">
        <span>{{ ticket.ticketNo }}</span>
        <strong>{{ statusText(ticket.status) }}</strong>
        <p>入场码：{{ ticket.qrCodeContent }}</p>
      </RouterLink>
    </section>
    <section v-else class="empty-inline">暂无电子票</section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getTickets } from '../api/ticketFlow'

const tickets = ref([])
const statusText = (status) => ({ UNUSED: '待入场', CHECKED_IN: '已入场', REFUNDED: '已退票', INVALID: '已失效', EXPIRED: '已过期' }[status] || status)

onMounted(async () => {
  tickets.value = await getTickets()
})
</script>

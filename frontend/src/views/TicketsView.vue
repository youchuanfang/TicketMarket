<template>
  <div class="page narrow">
    <SectionHeader title="我的票夹" eyebrow="电子票" />
    <section class="ticket-grid" v-if="tickets.length">
      <RouterLink v-for="ticket in tickets" :key="ticket.id" class="ticket-card" :to="`/tickets/${ticket.id}`">
        <img v-if="assetUrl(ticket.poster)" :src="assetUrl(ticket.poster)" :alt="ticket.itemTitle" class="ticket-poster" />
        <div v-else class="ticket-poster placeholder">暂无海报</div>
        <div class="ticket-card-body">
          <span>{{ ticket.ticketNo }}</span>
          <strong>{{ statusText(ticket.status) }}</strong>
          <h2>{{ ticket.itemTitle || '电子票' }}</h2>
          <p>{{ ticket.sessionTime || ticket.sessionName || '场次待确认' }}</p>
          <p>{{ ticket.venueName }} {{ ticket.cityName ? `/ ${ticket.cityName}` : '' }}</p>
          <p>{{ ticket.ticketLevelName || '票档待确认' }}{{ ticket.viewerName ? ` · ${ticket.viewerName}` : '' }}</p>
        </div>
      </RouterLink>
    </section>
    <section v-else class="empty-inline">暂无电子票</section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getTickets } from '../api/ticketFlow'
import { assetUrl } from '../utils/assets'

const tickets = ref([])
const statusText = (status) => ({ UNUSED: '待入场', CHECKED_IN: '已入场', REFUNDED: '已退票', INVALID: '已失效', EXPIRED: '已过期' }[status] || status)

onMounted(async () => {
  tickets.value = await getTickets()
})
</script>

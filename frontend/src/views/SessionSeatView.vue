<template>
  <div class="page narrow">
    <SectionHeader title="图形化选座" eyebrow="在线选座" />
    <section class="seat-purchase-layout">
      <div class="seat-purchase-main">
        <div class="stage-line">舞台 / 银幕</div>
        <svg viewBox="0 0 760 560" class="seat-map">
          <circle
            v-for="seat in seats"
            :key="seat.id"
            :cx="seat.x"
            :cy="seat.y"
            r="8"
            :fill="seatColor(seat)"
            :class="['seat-dot', { clickable: seat.status === 'AVAILABLE' }]"
            @click="toggleSeat(seat)"
          >
            <title>{{ seat.seatLabel }}：{{ statusText(seat.status) }}</title>
          </circle>
        </svg>
        <div class="seat-legend">
          <span v-for="item in legends" :key="item.status"><i :style="{ background: item.color }" />{{ item.label }}</span>
        </div>
      </div>
      <aside class="seat-summary">
        <h2>已选座位</h2>
        <p v-if="!selectedSeats.length">请选择可售座位</p>
        <div v-for="seat in selectedSeats" :key="seat.id" class="selected-seat">
          <span>{{ seat.seatLabel }}</span>
          <strong>¥{{ levelPrice(seat.ticketLevelId) }}</strong>
        </div>
        <div class="seat-total">
          <span>合计</span>
          <strong>¥{{ totalPrice }}</strong>
        </div>
        <el-button type="primary" size="large" :disabled="!selectedSeats.length">确认选座</el-button>
        <p class="seat-tip">本页用于选座状态确认，订单与支付将在购票流程中继续完成。</p>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { getSessionSeats, getSessionTicketLevels } from '../api/portal'

const route = useRoute()
const seats = ref([])
const ticketLevels = ref([])
const selectedIds = ref([])

const legends = [
  { status: 'AVAILABLE', label: '可售', color: '#2f9e44' },
  { status: 'LOCKED', label: '锁定', color: '#f59f00' },
  { status: 'SOLD', label: '已售', color: '#868e96' },
  { status: 'DISABLED', label: '不可售', color: '#ced4da' },
  { status: 'UNRELEASED', label: '未开放', color: '#adb5bd' },
  { status: 'POST_LOCK_RETURNED', label: '锁票后回收', color: '#7048e8' }
]

const colorMap = Object.fromEntries(legends.map((item) => [item.status, item.color]))
const selectedSeats = computed(() => seats.value.filter((seat) => selectedIds.value.includes(seat.id)))
const totalPrice = computed(() => selectedSeats.value.reduce((sum, seat) => sum + Number(levelPrice(seat.ticketLevelId)), 0))

const seatColor = (seat) => selectedIds.value.includes(seat.id) ? '#d9303e' : (colorMap[seat.status] || '#2f9e44')
const statusText = (status) => legends.find((item) => item.status === status)?.label || status
const levelPrice = (levelId) => ticketLevels.value.find((level) => level.id === levelId)?.price || 0

const toggleSeat = (seat) => {
  if (seat.status !== 'AVAILABLE') {
    ElMessage.warning(`${seat.seatLabel}当前不可选择`)
    return
  }
  if (selectedIds.value.includes(seat.id)) {
    selectedIds.value = selectedIds.value.filter((id) => id !== seat.id)
  } else {
    selectedIds.value = [...selectedIds.value, seat.id]
  }
}

onMounted(async () => {
  seats.value = await getSessionSeats(route.params.id)
  ticketLevels.value = await getSessionTicketLevels(route.params.id)
})
</script>

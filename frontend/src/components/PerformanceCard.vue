<template>
  <RouterLink class="event-card" :to="`/performances/${item.id}`">
    <img :src="item.poster" :alt="item.title" class="event-poster" />
    <div class="event-body">
      <div class="event-status" :class="item.saleStatus">{{ statusText }}</div>
      <h3>{{ item.title }}</h3>
      <p class="muted">{{ item.city }} · {{ item.venue }}</p>
      <p class="time">{{ item.startTime }}</p>
      <div class="card-footer">
        <strong>¥{{ item.priceMin }}起</strong>
        <span>{{ modeText }}</span>
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
})

const statusMap = {
  ON_SALE: '正在售票',
  COMING_SOON: '即将开售',
  RETURNED: '票量紧张',
  LOCKED: '已结束'
}

const modeMap = {
  SELECTABLE: '支持选座',
  AUTO_ALLOCATE: '自动分配',
  AREA_ONLY: '区域购票',
  STANDING: '站席'
}

const statusText = computed(() => statusMap[props.item.saleStatus] || props.item.saleStatus)
const modeText = computed(() => modeMap[props.item.saleMode] || props.item.saleMode)
</script>

<template>
  <div class="page narrow">
    <SectionHeader title="订单详情" eyebrow="购票记录" />
    <section v-if="order" class="detail-card">
      <h2>{{ order.ticketLevelName }}</h2>
      <p>订单号：{{ order.orderNo }}</p>
      <p>数量：{{ order.quantity }} 张</p>
      <p>金额：¥{{ order.totalAmount }}</p>
      <p>状态：{{ statusText(order.status) }}</p>
      <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="router.push(`/payment/${order.id}`)">继续支付</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SectionHeader from '../components/SectionHeader.vue'
import { getOrder } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const statusText = (status) => ({ PENDING_PAYMENT: '待支付', PAID: '已支付', TICKET_ISSUED: '已出票', CANCELLED: '已取消' }[status] || status)

onMounted(async () => {
  order.value = await getOrder(route.params.id)
})
</script>

<template>
  <div class="page narrow">
    <SectionHeader title="订单确认" eyebrow="待支付订单" />
    <section class="flow-panel" v-if="order">
      <div class="flow-main">
        <h2>{{ order.ticketLevelName }}</h2>
        <p>订单号：{{ order.orderNo }}</p>
        <p>数量：{{ order.quantity }} 张</p>
        <p>支付截止：{{ order.expireTime }}</p>
        <el-tag>{{ statusText(order.status) }}</el-tag>
      </div>
      <aside class="flow-aside">
        <p>应付金额</p>
        <strong>¥{{ order.totalAmount }}</strong>
        <el-button type="primary" size="large" @click="router.push(`/payment/${order.id}`)">去支付</el-button>
        <el-button plain @click="cancel">取消订单</el-button>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { cancelOrder, getOrder } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const statusText = (status) => ({ PENDING_PAYMENT: '待支付', PAID: '已支付', TICKET_ISSUED: '已出票', CANCELLED: '已取消' }[status] || status)

const cancel = async () => {
  order.value = await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
}

onMounted(async () => {
  order.value = await getOrder(route.params.orderId)
})
</script>

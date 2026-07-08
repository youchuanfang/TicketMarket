<template>
  <div class="page narrow">
    <SectionHeader title="订单详情" eyebrow="购票记录" />
    <section v-if="order" class="detail-card order-detail-card">
      <img v-if="assetUrl(order.poster)" :src="assetUrl(order.poster)" :alt="order.itemTitle" class="detail-poster" />
      <div>
        <h2>{{ order.itemTitle || order.ticketLevelName }}</h2>
        <p>订单号：{{ order.orderNo }}</p>
        <p>场次：{{ order.sessionTime || order.sessionName || '待确认' }}</p>
        <p v-if="order.venueName">场馆：{{ order.venueName }}</p>
        <p>票档：{{ order.ticketLevelName }}</p>
        <p>数量：{{ order.quantity }} 张</p>
        <p>金额：￥{{ order.totalAmount }}</p>
        <p>状态：{{ statusText(order.status) }}</p>
        <div class="detail-actions">
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="router.push(`/payment/${order.id}`)">继续支付</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" plain @click="cancel">取消订单</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { cancelOrder, getOrder } from '../api/ticketFlow'
import { assetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const statusText = (status) => ({
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  TICKET_ISSUED: '已出票',
  REFUND_APPLYING: '退票审核中',
  REFUNDED: '已退票',
  CANCELLED: '已取消'
}[status] || status)

onMounted(async () => {
  order.value = await getOrder(route.params.id)
})

const cancel = async () => {
  order.value = await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
}
</script>

<style scoped>
.order-detail-card {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 18px;
}

.detail-poster {
  width: 140px;
  height: 186px;
  object-fit: cover;
  border-radius: 8px;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>

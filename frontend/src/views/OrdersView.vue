<template>
  <div class="page narrow">
    <SectionHeader title="我的订单" eyebrow="购票记录" />
    <section class="order-list">
      <el-empty v-if="!orders.length" description="暂无订单" />
      <article v-for="order in orders" :key="order.id" class="order-card">
        <img v-if="assetUrl(order.poster)" :src="assetUrl(order.poster)" :alt="order.itemTitle" class="order-poster" />
        <div v-else class="order-poster placeholder">暂无海报</div>
        <div class="order-main">
          <div class="order-title-row">
            <h2>{{ order.itemTitle || '订单项目' }}</h2>
            <el-tag>{{ statusText(order.status) }}</el-tag>
          </div>
          <p class="muted">订单号：{{ order.orderNo }}</p>
          <p>{{ order.sessionTime || order.sessionName || '场次待确认' }}<span v-if="order.venueName"> · {{ order.venueName }}</span></p>
          <p>{{ order.ticketLevelName }} · ￥{{ order.unitPrice || order.totalAmount }} × {{ order.quantity }}</p>
          <strong class="order-amount">合计 ￥{{ order.totalAmount }}</strong>
        </div>
        <div class="order-actions">
          <RouterLink class="table-link" :to="`/orders/${order.id}`">详情</RouterLink>
          <RouterLink v-if="order.status === 'PENDING_PAYMENT'" class="table-link" :to="`/payment/${order.id}`">支付</RouterLink>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" link type="danger" @click="cancel(order)">取消订单</el-button>
          <el-button v-if="order.status === 'TICKET_ISSUED'" link type="warning" @click="refund(order)">退票</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { cancelOrder, getOrders } from '../api/ticketFlow'
import { applyRefund } from '../api/operations'
import { assetUrl } from '../utils/assets'

const orders = ref([])
const statusText = (status) => ({
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  TICKET_ISSUED: '已出票',
  REFUND_APPLYING: '退票审核中',
  REFUNDED: '已退票',
  CANCELLED: '已取消',
  TIMEOUT_CLOSED: '已关闭'
}[status] || status)

const loadOrders = async () => {
  orders.value = await getOrders()
}

onMounted(loadOrders)

const refund = async (row) => {
  try {
    await applyRefund(row.id)
    ElMessage.success('退票申请已提交')
    await loadOrders()
  } catch (error) {
    const message = /退票|refund|截止|超/.test(error.message || '') ? '已过退票期，不支持退票' : error.message
    ElMessage.error(message)
  }
}

const cancel = async (row) => {
  await cancelOrder(row.id)
  ElMessage.success('订单已取消')
  await loadOrders()
}
</script>

<style scoped>
.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  display: grid;
  grid-template-columns: 96px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.order-poster {
  width: 96px;
  height: 128px;
  object-fit: cover;
  border-radius: 6px;
  background: #f3f4f6;
}

.order-poster.placeholder {
  display: grid;
  place-items: center;
  color: #8a94a6;
  font-size: 13px;
}

.order-main h2 {
  margin: 0;
  font-size: 18px;
}

.order-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.muted {
  color: #6b7280;
}

.order-amount {
  color: #d9303e;
}

.order-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
</style>

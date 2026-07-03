<template>
  <div class="page narrow">
    <SectionHeader title="我的订单" eyebrow="购票记录" />
    <section class="list-panel">
      <el-table :data="orders" border empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="ticketLevelName" label="票档" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <RouterLink class="table-link" :to="`/orders/${row.id}`">详情</RouterLink>
            <RouterLink v-if="row.status === 'PENDING_PAYMENT'" class="table-link" :to="`/payment/${row.id}`">支付</RouterLink>
            <el-button v-if="row.status === 'PENDING_PAYMENT'" link type="danger" @click="cancel(row)">取消订单</el-button>
            <el-button v-if="row.status === 'TICKET_ISSUED'" link type="warning" @click="refund(row)">退票</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { cancelOrder, getOrders } from '../api/ticketFlow'
import { applyRefund } from '../api/operations'

const orders = ref([])
const statusText = (status) => ({ PENDING_PAYMENT: '待支付', PAID: '已支付', TICKET_ISSUED: '已出票', CANCELLED: '已取消', TIMEOUT_CLOSED: '已关闭' }[status] || status)

onMounted(async () => {
  orders.value = await getOrders()
})

const refund = async (row) => {
  await applyRefund(row.id)
  ElMessage.success('退票申请已提交')
  orders.value = await getOrders()
}

const cancel = async (row) => {
  await cancelOrder(row.id)
  ElMessage.success('订单已取消')
  orders.value = await getOrders()
}
</script>

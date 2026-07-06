<template>
  <div class="page narrow">
    <section v-if="success" class="flow-status">
      <el-result :icon="success ? 'success' : 'warning'" :title="result?.message || '正在查询抢票结果'" :sub-title="statusText">
        <template #extra>
          <el-button type="primary" @click="router.push(`/order/confirm/${result.orderId}`)">去支付</el-button>
        </template>
      </el-result>
    </section>
    <section v-else class="flow-status">
      <div class="failure-card">
        <h1>不要气馁，请继续尝试</h1>
        <p>当前票源紧张，你可以继续尝试或选择其他票档。</p>
        <div class="action-row">
          <el-button type="primary" @click="retry">继续尝试</el-button>
          <el-button @click="backToPurchase">返回购票</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRushResult } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const result = ref(null)
const success = computed(() => result.value?.status === 'SUCCESS')
const statusText = computed(() => ({
  SUCCESS: '请在 5 分钟内完成支付',
  SOLD_OUT: '本轮票源已售罄',
  LOCKED: '当前场次暂不可售',
  DUPLICATE: '检测到重复提交，请查看已有订单',
  NOT_STARTED: '本轮售票尚未开始',
  LIMITED: '已超出限购规则',
  NO_AUTH: '请先完成实名认证'
}[result.value?.status] || '当前网络不稳定，请稍后查询抢票结果'))
const retry = () => {
  const performanceId = result.value?.performanceId
  router.push(performanceId ? `/performance/${performanceId}/purchase?sessionId=${result.value.sessionId}` : '/search')
}
const backToPurchase = () => {
  const performanceId = result.value?.performanceId
  router.push(performanceId ? `/performance/${performanceId}/purchase?sessionId=${result.value.sessionId}` : '/search')
}

onMounted(async () => {
  result.value = await getRushResult(route.params.requestId)
})
</script>

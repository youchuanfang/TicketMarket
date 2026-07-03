<template>
  <div class="page narrow">
    <section class="flow-status">
      <el-result :icon="success ? 'success' : 'warning'" :title="result?.message || '正在查询抢票结果'" :sub-title="statusText">
        <template #extra>
          <el-button v-if="success" type="primary" @click="router.push(`/order/confirm/${result.orderId}`)">去支付</el-button>
          <el-button v-else @click="router.push('/')">返回首页</el-button>
        </template>
      </el-result>
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
  LOCKED: '本轮售票已锁票',
  DUPLICATE: '检测到重复提交，请查看已有订单',
  NOT_STARTED: '本轮售票尚未开始',
  LIMITED: '已超出限购规则',
  NO_AUTH: '请先完成实名认证'
}[result.value?.status] || '当前网络不稳定，请稍后查询抢票结果'))

onMounted(async () => {
  result.value = await getRushResult(route.params.requestId)
})
</script>

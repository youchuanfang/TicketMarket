<template>
  <div class="page narrow">
    <section class="flow-status">
      <div class="loader-ring" />
      <h1>正在为你锁票</h1>
      <p>请勿刷新页面，系统正在确认库存和座位。</p>
      <div class="queue-metric">
        <span>前方人数</span>
        <strong>{{ peopleAhead }}</strong>
      </div>
      <el-button type="primary" @click="goResult">查看结果</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRushResult } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const peopleAhead = ref(18)

const goResult = () => router.push(`/rush/result/${route.params.requestId}`)

onMounted(() => {
  const timer = window.setInterval(() => {
    peopleAhead.value = Math.max(0, peopleAhead.value - 6)
  }, 500)
  window.setTimeout(async () => {
    window.clearInterval(timer)
    await getRushResult(route.params.requestId)
    goResult()
  }, 1800)
})
</script>

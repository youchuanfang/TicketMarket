<template>
  <div class="page narrow">
    <section class="flow-status">
      <div class="loader-ring" />
      <h1>正在为你锁票</h1>
      <p>请勿刷新页面，系统正在确认库存和座位。</p>
      <el-button type="primary" @click="goResult">查看结果</el-button>
    </section>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRushResult } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()

const goResult = () => router.push(`/rush/result/${route.params.requestId}`)

onMounted(() => {
  window.setTimeout(async () => {
    await getRushResult(route.params.requestId)
    goResult()
  }, 1800)
})
</script>

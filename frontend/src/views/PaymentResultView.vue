<template>
  <div class="page narrow">
    <section class="flow-status">
      <el-result icon="success" title="支付成功，电子票已生成" sub-title="你可以在我的票夹查看入场二维码。">
        <template #extra>
          <el-button type="primary" @click="router.push('/tickets')">查看票夹</el-button>
          <el-button @click="router.push('/orders')">查看订单</el-button>
        </template>
      </el-result>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="page narrow">
    <SectionHeader title="模拟支付" eyebrow="安全确认" />
    <section class="flow-panel" v-if="order">
      <div class="flow-main">
        <h2>选择支付方式</h2>
        <el-radio-group v-model="payMethod">
          <el-radio-button label="MOCK_ALIPAY">模拟支付宝</el-radio-button>
          <el-radio-button label="MOCK_WECHAT">模拟微信</el-radio-button>
          <el-radio-button label="BALANCE">余额支付</el-radio-button>
        </el-radio-group>
        <p class="seat-tip">当前页面只展示支付确认结果，不会发起真实扣款。</p>
      </div>
      <aside class="flow-aside">
        <p>支付金额</p>
        <strong>¥{{ order.totalAmount }}</strong>
        <el-button type="primary" size="large" :loading="paying" @click="pay">确认支付</el-button>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SectionHeader from '../components/SectionHeader.vue'
import { getOrder, payOrder } from '../api/ticketFlow'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const payMethod = ref('MOCK_ALIPAY')
const paying = ref(false)

const pay = async () => {
  paying.value = true
  try {
    await payOrder(route.params.orderId, { payMethod: payMethod.value })
    router.push(`/payment/result/${route.params.orderId}`)
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  order.value = await getOrder(route.params.orderId)
})
</script>

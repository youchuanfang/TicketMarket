<template>
  <div class="page narrow">
    <SectionHeader title="个人中心" eyebrow="Profile" />
    <section class="profile-card">
      <div>
        <p class="eyebrow">当前账号</p>
        <h2>{{ user.profile?.nickname || user.profile?.username }}</h2>
        <p>{{ user.profile?.username }} · {{ roleText }}</p>
      </div>
      <el-tag :type="user.profile?.realNameVerified ? 'success' : 'warning'">
        {{ user.profile?.realNameVerified ? '已实名' : '未实名' }}
      </el-tag>
    </section>

    <section class="profile-info-grid">
      <div class="info-card">
        <span>手机号</span>
        <strong>{{ user.profile?.phoneMasked || '138****0000' }}</strong>
      </div>
      <div class="info-card">
        <span>邮箱</span>
        <strong>{{ user.profile?.email || '未绑定' }}</strong>
      </div>
      <div class="info-card">
        <span>实名证件</span>
        <strong>{{ user.profile?.idCardMasked || '未认证' }}</strong>
      </div>
    </section>

    <section class="detail-section">
      <SectionHeader title="常用入口" />
      <div class="quick-entry-grid">
        <RouterLink v-for="entry in entries" :key="entry.path" :to="entry.path" class="quick-entry">
          <el-icon><component :is="entry.icon" /></el-icon>
          <span>{{ entry.title }}</span>
          <small>{{ entry.desc }}</small>
        </RouterLink>
      </div>
    </section>

    <section class="detail-section">
      <SectionHeader title="实名认证" />
      <div v-if="user.profile?.realNameVerified" class="verified-box">
        <el-icon><CircleCheckFilled /></el-icon>
        <div>
          <strong>实名认证已完成</strong>
          <p>证件信息已脱敏展示：{{ user.profile?.idCardMasked }}</p>
        </div>
      </div>
      <div v-else class="inline-form">
        <el-input v-model="realName.realName" placeholder="真实姓名" />
        <el-input v-model="realName.idCard" placeholder="身份证号" />
        <el-button type="primary" @click="submitRealNameInfo">去实名认证</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { submitRealName } from '../api/auth'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const realName = reactive({ realName: '', idCard: '' })

const roleMap = {
  ADMIN: '系统管理员',
  MANAGER: '票务管理员',
  CHECKER: '检票员',
  USER: '普通用户'
}

const roleText = computed(() => roleMap[user.role] || user.role)

const entries = [
  { title: '实名认证', desc: '完善实名信息', path: '/real-name', icon: 'UserFilled' },
  { title: '观演人管理', desc: '维护常用观演人', path: '/viewers', icon: 'Avatar' },
  { title: '我的订单', desc: '查看购票记录', path: '/orders', icon: 'Tickets' },
  { title: '我的票夹', desc: '查看电子票', path: '/tickets', icon: 'Wallet' },
  { title: '我的消息', desc: '查看站内通知', path: '/messages', icon: 'Message' }
]

const submitRealNameInfo = async () => {
  try {
    const profile = await submitRealName(realName)
    user.userInfo = profile
    localStorage.setItem('ticket-market-user', JSON.stringify(profile))
    ElMessage.success('实名认证已完成')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  await user.fetchMe(true).catch(() => user.clearAuth())
})
</script>

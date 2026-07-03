<template>
  <div class="page narrow">
    <section class="forbidden-page">
      <el-icon><WarningFilled /></el-icon>
      <h1>无权限访问</h1>
      <p>当前账号没有访问该页面的权限，请切换账号或返回前台继续浏览。</p>
      <div class="forbidden-actions">
        <RouterLink class="primary-link" to="/">返回首页</RouterLink>
        <RouterLink v-if="!user.isLoggedIn" class="primary-link" :to="{ path: '/login', query: { redirect: route.query.from || '/' } }">去登录</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const user = useUserStore()
</script>

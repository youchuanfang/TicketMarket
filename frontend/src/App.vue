<template>
  <div class="app-shell">
    <header v-if="!isAdminRoute" class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">T</span>
        <span>星票</span>
      </RouterLink>
      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/category/concert">演唱会</RouterLink>
        <RouterLink to="/category/movie">电影</RouterLink>
        <RouterLink to="/category/drama">话剧歌剧</RouterLink>
        <RouterLink v-if="user.canEnterAdmin" to="/admin">后台</RouterLink>
      </nav>
      <RouterLink class="city-switch" to="/city">
        <el-icon><Location /></el-icon>
        {{ currentCity }}
      </RouterLink>
      <div class="global-search">
        <el-input v-model="keyword" placeholder="搜索演出、电影、场馆" clearable @keyup.enter="goSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="top-actions">
        <template v-if="user.isLoggedIn">
          <RouterLink to="/user">{{ displayName }}</RouterLink>
          <RouterLink to="/orders">我的订单</RouterLink>
          <RouterLink to="/tickets">我的票夹</RouterLink>
          <RouterLink to="/messages">消息</RouterLink>
          <RouterLink to="/viewers">观演人</RouterLink>
          <button class="plain-button" @click="logout">退出登录</button>
        </template>
        <RouterLink v-else class="login-link" to="/login">登录/注册</RouterLink>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const keyword = ref('')
const currentCity = ref(localStorage.getItem('ticket-market-city') || '上海')

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const displayName = computed(() => user.profile?.nickname || user.profile?.username || '个人中心')

const goSearch = () => {
  router.push({ path: '/search', query: { keyword: keyword.value } })
}

const logout = async () => {
  await user.logout()
  router.push('/')
}

onMounted(() => {
  user.fetchMe().catch(() => user.clearAuth())
  const syncCity = () => {
    currentCity.value = localStorage.getItem('ticket-market-city') || '上海'
  }
  window.addEventListener('storage', syncCity)
  window.addEventListener('ticket-market-city-change', syncCity)
})
</script>

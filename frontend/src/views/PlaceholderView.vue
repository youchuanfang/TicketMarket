<template>
  <div class="page narrow">
    <SectionHeader :title="title" />
    <section class="empty-state">
      <el-icon><FolderOpened /></el-icon>
      <h3>{{ description }}</h3>
      <p>相关信息会在这里集中展示。</p>
      <RouterLink class="primary-link" to="/">继续逛逛</RouterLink>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SectionHeader from '../components/SectionHeader.vue'

const route = useRoute()
const title = computed(() => route.meta.title || '个人服务')
const description = computed(() => route.meta.description || '暂无数据')
</script>

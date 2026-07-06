<template>
  <div class="page narrow">
    <SectionHeader title="搜索结果" eyebrow="Search" />
    <div class="search-panel">
      <el-input v-model="form.keyword" placeholder="关键词、演出、场馆、地址" clearable />
      <el-select v-model="form.city" placeholder="城市" clearable>
        <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
      </el-select>
      <el-select v-model="form.category" placeholder="分类" clearable>
        <el-option v-for="item in categories" :key="item.code" :label="item.name" :value="item.code" />
      </el-select>
      <el-select v-model="form.status" placeholder="售票状态" clearable>
        <el-option label="正在售票" value="ON_SALE" />
        <el-option label="即将开售" value="COMING_SOON" />
        <el-option label="票量紧张" value="RETURNED" />
        <el-option label="已结束" value="LOCKED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
    </div>
    <p class="result-count">共找到 {{ total }} 个演出</p>
    <div class="event-grid">
      <PerformanceCard v-for="item in items" :key="item.id" :item="item" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PerformanceCard from '../components/PerformanceCard.vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getCategories, searchPerformances } from '../api/portal'

const route = useRoute()
const form = reactive({
  keyword: route.query.keyword || '',
  city: route.query.city || '',
  category: route.query.category || '',
  status: route.query.status || ''
})
const cities = ref([])
const categories = ref([])
const items = ref([])
const total = ref(0)

const load = async () => {
  try {
    const data = await searchPerformances(form)
    items.value = data.items
    total.value = data.total
    cities.value = data.filters?.cities || []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  categories.value = await getCategories()
  await load()
})
</script>

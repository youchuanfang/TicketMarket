<template>
  <div class="page narrow">
    <SectionHeader :title="cityMode ? '城市选择' : currentTitle" :eyebrow="cityMode ? 'City' : 'Category'" />
    <div v-if="cityMode" class="city-board">
      <button v-for="city in cities" :key="city" class="city-card" @click="goCity(city)">
        <el-icon><Location /></el-icon>
        {{ city }}
      </button>
    </div>
    <template v-else>
      <div class="filter-row">
        <el-select v-model="city" placeholder="城市" clearable>
          <el-option v-for="item in cities" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="status" placeholder="状态" clearable>
          <el-option label="正在售票" value="ON_SALE" />
          <el-option label="即将开售" value="COMING_SOON" />
          <el-option label="少量回流" value="RETURNED" />
          <el-option label="本轮锁票" value="LOCKED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">筛选</el-button>
      </div>
      <div class="event-grid">
        <PerformanceCard v-for="item in items" :key="item.id" :item="item" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PerformanceCard from '../components/PerformanceCard.vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getCategories, searchPerformances } from '../api/portal'

defineProps({ cityMode: Boolean })

const route = useRoute()
const router = useRouter()
const categories = ref([])
const items = ref([])
const city = ref(route.query.city || '')
const status = ref('')
const cities = ['上海', '杭州', '南京', '深圳']

const currentTitle = computed(() => {
  const code = route.params.code || ''
  return categories.value.find((item) => item.code === code)?.name || '全部分类'
})

const load = async () => {
  try {
    const code = route.params.code === 'movie' ? '' : route.params.code
    const data = await searchPerformances({ category: code, city: city.value, status: status.value })
    items.value = data.items
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const goCity = (value) => {
  router.push({ path: '/search', query: { city: value } })
}

onMounted(async () => {
  categories.value = await getCategories()
  await load()
})

watch(() => route.params.code, load)
</script>

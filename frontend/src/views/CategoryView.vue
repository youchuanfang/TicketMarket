<template>
  <div class="page narrow">
    <SectionHeader :title="cityMode ? '城市选择' : currentTitle" :eyebrow="cityMode ? 'City' : 'Category'" />
    <div v-if="cityMode" class="city-board">
      <button v-for="cityName in cities" :key="cityName" class="city-card" @click="goCity(cityName)">
        <el-icon><Location /></el-icon>
        {{ cityName }}
      </button>
    </div>
    <template v-else>
      <div class="filter-row">
        <el-select v-model="city" placeholder="城市" clearable>
          <el-option v-for="item in cities" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="status" placeholder="状态" clearable>
          <el-option label="热卖中" value="ON_SALE" />
          <el-option label="即将开售" value="COMING_SOON" />
          <el-option label="已售罄" value="SOLD_OUT" />
          <el-option label="已结束" value="ENDED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">筛选</el-button>
      </div>
      <div class="event-grid">
        <PerformanceCard v-for="item in items" :key="`${item.targetType || 'PERFORMANCE'}-${item.id}`" :item="item" />
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
import { getCategories, getMovies, searchPerformances } from '../api/portal'

defineProps({ cityMode: Boolean })

const route = useRoute()
const router = useRouter()
const categories = ref([])
const items = ref([])
const city = ref(route.query.city || '')
const status = ref('')
const cities = ref([])

const currentTitle = computed(() => {
  const code = route.params.code || ''
  if (code === 'movie') return '电影'
  return categories.value.find((item) => item.code === code)?.name || '全部分类'
})

const load = async () => {
  try {
    if (route.params.code === 'movie') {
      const movies = await getMovies()
      const normalized = movies.map((movie) => ({
        ...movie,
        targetType: 'MOVIE',
        targetId: movie.id,
        categoryName: '电影',
        categoryCode: 'movie',
        title: movie.title,
        city: movie.sessions?.[0]?.city || '',
        venueName: movie.sessions?.[0]?.cinemaName || '',
        startTime: movie.sessions?.[0]?.startTime || movie.releaseDate,
        minPrice: movie.sessions?.[0]?.price || 0,
        saleStatus: movie.sessions?.length ? 'ON_SALE' : 'COMING_SOON'
      }))
      items.value = normalized.filter((movie) => (
        (!city.value || movie.city === city.value) &&
        (!status.value || movie.saleStatus === status.value)
      ))
      cities.value = [...new Set(normalized.map((movie) => movie.city).filter(Boolean))]
      return
    }
    const data = await searchPerformances({ category: route.params.code, city: city.value, status: status.value })
    items.value = data.items
    cities.value = data.filters?.cities || []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const goCity = (value) => {
  localStorage.setItem('ticket-market-city', value)
  window.dispatchEvent(new Event('ticket-market-city-change'))
  router.push('/')
}

onMounted(async () => {
  categories.value = await getCategories()
  await load()
})

watch(() => route.params.code, load)
</script>

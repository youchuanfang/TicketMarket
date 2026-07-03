<template>
  <div class="page detail-page" v-if="movie">
    <section class="detail-hero">
      <img :src="movie.poster" :alt="movie.title" class="detail-poster" />
      <div class="detail-main">
        <p class="eyebrow">电影 · 选座购票</p>
        <h1>{{ movie.title }}</h1>
        <p class="summary">{{ movie.summary }}</p>
        <div class="detail-meta">
          <span>{{ movie.genre }}</span>
          <span>{{ movie.releaseDate }} 上映</span>
          <span>{{ movie.durationMinutes }} 分钟</span>
        </div>
        <p>导演：{{ movie.director }}</p>
        <p>主演：{{ movie.actors }}</p>
      </div>
    </section>
    <section class="detail-section">
      <SectionHeader title="放映场次" />
      <div class="session-grid">
        <button v-for="session in movie.sessions" :key="session.id" class="session-card">
          <strong>{{ session.startTime }}</strong>
          <span>{{ session.hallName }} · 支持选座</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { getMovie } from '../api/portal'

const route = useRoute()
const movie = ref(null)

onMounted(async () => {
  try {
    movie.value = await getMovie(route.params.id)
  } catch (error) {
    ElMessage.error(error.message)
  }
})
</script>

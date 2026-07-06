<template>
  <div class="page detail-page" v-if="movie">
    <section class="detail-hero">
      <img :src="assetUrl(movie.poster)" :alt="movie.title" class="detail-poster" />
      <div class="detail-main">
        <p class="eyebrow">电影 · 选座购票</p>
        <h1>{{ movie.title }}</h1>
        <p class="summary">{{ movie.summary }}</p>
        <div class="detail-meta">
          <span>{{ movie.genre }}</span>
          <span>{{ movie.releaseDate }} 上映</span>
          <span>{{ movie.durationMinutes }} 分钟</span>
          <span v-if="movie.rating">评分 {{ movie.rating }}</span>
        </div>
        <p>导演：{{ movie.director }}</p>
        <p>主演：{{ movie.actors }}</p>
      </div>
    </section>

    <section class="detail-section movie-flow">
      <SectionHeader :title="`${currentCity} 放映影院`" />
      <div class="date-strip">
        <button
          v-for="date in dateOptions"
          :key="date"
          :class="{ active: selectedDate === date }"
          @click="selectedDate = date; selectedCinemaId = null"
        >
          {{ dateLabel(date) }}
        </button>
      </div>

      <div v-if="cinemasForDate.length" class="cinema-list">
        <button
          v-for="cinema in cinemasForDate"
          :key="cinema.cinemaId"
          :class="{ active: selectedCinemaId === cinema.cinemaId }"
          @click="selectedCinemaId = cinema.cinemaId"
        >
          <div>
            <strong>{{ cinema.cinemaName }}</strong>
            <p>{{ cinema.address }}</p>
            <span>近期场次：{{ timesForCinema(cinema).join(' / ') }}</span>
          </div>
          <em>¥{{ minPrice(cinema) }}起</em>
        </button>
      </div>
      <p v-else class="empty-inline">当前城市当天暂无排片，请切换日期或城市。</p>

      <div v-if="selectedCinema" class="cinema-session-panel">
        <div class="movie-poster-strip">
          <button class="active">
            <img :src="assetUrl(movie.poster)" :alt="movie.title" />
            <span>{{ movie.title }}</span>
          </button>
        </div>
        <h3>{{ selectedCinema.cinemaName }} · {{ selectedDate }}</h3>
        <div class="session-list">
          <button
            v-for="session in sessionsForSelectedCinema"
            :key="session.sessionId"
            class="movie-session-row"
            @click="router.push(`/session/${session.sessionId}/seats?movie=1`)"
          >
            <strong>{{ timeOnly(session.startTime) }}</strong>
            <span>{{ session.hallName }} · 支持选座</span>
            <em>¥{{ session.price }}</em>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SectionHeader from '../components/SectionHeader.vue'
import { getMovie, getMovieSchedule } from '../api/portal'
import { assetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const movie = ref(null)
const schedule = ref({ dates: [], cinemas: [], sessions: [] })
const currentCity = ref(localStorage.getItem('ticket-market-city') || '上海')
const selectedDate = ref('')
const selectedCinemaId = ref(null)

const dateOptions = computed(() => schedule.value.dates?.length ? schedule.value.dates : fallbackDates(movie.value?.releaseDate))
const cinemasForDate = computed(() => (schedule.value.cinemas || [])
  .map((cinema) => ({ ...cinema, sessions: (cinema.sessions || []).filter((session) => session.showDate === selectedDate.value) }))
  .filter((cinema) => cinema.sessions.length))
const selectedCinema = computed(() => cinemasForDate.value.find((cinema) => cinema.cinemaId === selectedCinemaId.value))
const sessionsForSelectedCinema = computed(() => [...(selectedCinema.value?.sessions || [])].sort((a, b) => a.startTime.localeCompare(b.startTime)))

const fallbackDates = (start) => {
  if (!start) return []
  const base = new Date(`${start}T00:00:00`)
  return Array.from({ length: 5 }, (_, index) => {
    const next = new Date(base)
    next.setDate(base.getDate() + index)
    return next.toISOString().slice(0, 10)
  })
}

const dateLabel = (date) => `${date.slice(5, 7)}月${date.slice(8, 10)}日`
const timeOnly = (value) => String(value || '').slice(11, 16)
const timesForCinema = (cinema) => cinema.sessions.map((session) => timeOnly(session.startTime)).slice(0, 4)
const minPrice = (cinema) => Math.min(...cinema.sessions.map((session) => Number(session.price || 0))).toFixed(0)

onMounted(async () => {
  try {
    movie.value = await getMovie(route.params.id)
    schedule.value = await getMovieSchedule(route.params.id, { city: currentCity.value })
    selectedDate.value = dateOptions.value[0] || ''
  } catch (error) {
    ElMessage.error(error.message)
  }
})
</script>

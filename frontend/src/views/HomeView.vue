<template>
  <div class="page">
    <section class="hero-band">
      <el-carousel height="340px" indicator-position="outside" arrow="always">
        <el-carousel-item v-for="banner in home.banners" :key="`${banner.targetType}-${banner.targetId}`">
          <RouterLink class="hero-slide" :to="banner.detailPath || (banner.targetType === 'MOVIE' ? `/movies/${banner.targetId}` : `/performances/${banner.targetId}`)">
            <img :src="assetUrl(banner.poster || banner.image)" :alt="banner.title" />
            <div class="hero-copy">
              <p class="eyebrow">TicketMarket</p>
              <h1>{{ banner.title }}</h1>
              <p>{{ banner.city }} · {{ banner.venue }} · {{ banner.startTime }}</p>
            </div>
          </RouterLink>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="content-band">
      <div class="category-grid">
        <RouterLink v-for="category in home.categories" :key="category.code" :to="`/category/${category.code}`" class="category-chip">
          <el-icon><component :is="category.icon" /></el-icon>
          <span>{{ category.name }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="content-band">
      <SectionHeader eyebrow="Hot Picks" title="热门推荐" to="/search?status=ON_SALE" />
      <div class="event-grid">
        <PerformanceCard v-for="item in home.hot" :key="item.id" :item="item" />
      </div>
    </section>

    <section class="content-band">
      <SectionHeader title="即将开售" to="/search?status=COMING_SOON" />
      <div class="compact-list">
        <RouterLink v-for="item in home.comingSoon" :key="item.id" :to="`/performances/${item.id}`" class="compact-item">
          <span>{{ item.title }}</span>
          <strong>{{ item.startTime }}</strong>
        </RouterLink>
      </div>
    </section>

    <section class="content-band">
      <SectionHeader title="热卖中" to="/search?status=ON_SALE" />
      <div class="event-grid">
        <PerformanceCard v-for="item in home.onSale" :key="item.id" :item="item" />
      </div>
    </section>

    <section v-for="section in home.categorySections" :key="section.code" class="content-band">
      <SectionHeader :title="section.name" :to="`/category/${section.code}`" />
      <div class="event-grid">
        <PerformanceCard v-for="item in section.items" :key="item.id" :item="item" />
      </div>
    </section>

    <section class="content-band">
      <SectionHeader title="电影热映" to="/category/movie" />
      <div class="movie-row">
        <RouterLink v-for="movie in home.movies" :key="movie.id" class="movie-card" :to="movie.detailPath || `/movies/${movie.id}`">
          <img :src="assetUrl(movie.poster)" :alt="movie.title" />
          <strong>{{ movie.title }}</strong>
          <span>{{ movie.genre }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="portal-meta">
      <div>
        <h3>热门城市</h3>
        <p>{{ home.hotCities?.join(' / ') }}</p>
      </div>
      <div>
        <h3>热门场馆</h3>
        <p>{{ home.hotVenues?.join(' / ') }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PerformanceCard from '../components/PerformanceCard.vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getHome } from '../api/portal'
import { assetUrl } from '../utils/assets'

const home = reactive({
  banners: [],
  categories: [],
  hot: [],
  comingSoon: [],
  onSale: [],
  categorySections: [],
  movies: [],
  hotCities: [],
  hotVenues: []
})

onMounted(async () => {
  try {
    Object.assign(home, await getHome())
  } catch (error) {
    ElMessage.error(error.message)
  }
})
</script>

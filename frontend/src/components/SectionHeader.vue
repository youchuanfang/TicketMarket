<template>
  <div class="section-header">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
    </div>
    <RouterLink v-if="to" :to="to" class="section-more">查看更多</RouterLink>
  </div>
</template>

<script setup>
defineProps({
  eyebrow: String,
  title: {
    type: String,
    required: true
  },
  to: String
})
</script>

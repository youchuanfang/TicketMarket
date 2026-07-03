<template>
  <div class="status-panel" :class="type">
    <el-icon><component :is="icon" /></el-icon>
    <div>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'info'
  },
  icon: {
    type: String,
    default: 'InfoFilled'
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  }
})
</script>

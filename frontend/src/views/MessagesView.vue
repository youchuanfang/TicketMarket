<template>
  <div class="page narrow">
    <SectionHeader title="我的消息" eyebrow="站内通知" />
    <section class="list-panel">
      <div v-if="!messages.length" class="empty-inline">暂无消息</div>
      <article v-for="message in messages" :key="message.id" class="message-item">
        <div>
          <h2>{{ message.title }}</h2>
          <p>{{ message.content }}</p>
          <span>{{ message.createdAt }}</span>
        </div>
        <el-button v-if="!message.read" plain @click="markRead(message)">标为已读</el-button>
        <el-tag v-else>已读</el-tag>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import SectionHeader from '../components/SectionHeader.vue'
import { getMessages, readMessage } from '../api/operations'

const messages = ref([])

const load = async () => {
  messages.value = await getMessages()
}

const markRead = async (message) => {
  await readMessage(message.id)
  await load()
}

onMounted(load)
</script>

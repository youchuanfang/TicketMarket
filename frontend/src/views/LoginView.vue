<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <p class="eyebrow">TicketMarket</p>
        <h1>欢迎来到星票</h1>
        <p>登录后可管理实名信息、观演人、订单、票夹和站内消息。平台支持演出、电影、展览与体育赛事的一站式票务服务。</p>
        <div class="auth-feature-list">
          <span><el-icon><Tickets /></el-icon>电子票入场</span>
          <span><el-icon><UserFilled /></el-icon>实名观演人</span>
          <span><el-icon><Bell /></el-icon>开售提醒</span>
        </div>
      </div>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top">
            <el-form-item label="用户名"><el-input v-model="loginForm.username" autocomplete="username" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="loginForm.password" type="password" autocomplete="current-password" show-password /></el-form-item>
            <el-button type="primary" size="large" @click="submitLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-position="top">
            <el-form-item label="用户名"><el-input v-model="registerForm.username" autocomplete="username" /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="registerForm.nickname" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="registerForm.password" type="password" autocomplete="new-password" show-password /></el-form-item>
            <el-button type="primary" size="large" @click="submitRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tab = ref('login')
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

const redirectAfterLogin = () => {
  router.push(route.query.redirect || '/user')
}

const submitLogin = async () => {
  try {
    await user.login(loginForm)
    ElMessage.success('登录成功')
    redirectAfterLogin()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const submitRegister = async () => {
  try {
    await user.register(registerForm)
    ElMessage.success('注册成功')
    redirectAfterLogin()
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

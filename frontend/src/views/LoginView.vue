<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <p class="eyebrow">TicketMarket</p>
        <h1>欢迎来到星票</h1>
        <p>普通用户、管理员和检票员使用独立入口登录，账号权限互不混用。</p>
        <div class="role-entry">
          <button v-for="entry in roleEntries" :key="entry.role" :class="{ active: loginRole === entry.role }" @click="loginRole = entry.role">
            <strong>{{ entry.title }}</strong>
            <span>{{ entry.desc }}</span>
          </button>
        </div>
      </div>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top">
            <el-form-item label="账号"><el-input v-model="loginForm.username" autocomplete="username" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="loginForm.password" type="password" autocomplete="current-password" show-password /></el-form-item>
            <el-button type="primary" size="large" @click="submitLogin">{{ currentRoleTitle }}登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="普通用户注册" name="register">
          <el-form :model="registerForm" label-position="top">
            <el-form-item label="用户名"><el-input v-model="registerForm.username" autocomplete="username" /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="registerForm.nickname" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="registerForm.password" type="password" autocomplete="new-password" show-password /></el-form-item>
            <el-button type="primary" size="large" @click="submitRegister">注册普通用户并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tab = ref('login')
const loginRole = ref('USER')
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '' })
const roleEntries = [
  { role: 'USER', title: '普通用户', desc: '购票、退票、查看订单' },
  { role: 'ADMIN', title: '管理员', desc: '后台运营管理' },
  { role: 'CHECKER', title: '检票员', desc: '现场验票核验' }
]

const currentRoleTitle = computed(() => roleEntries.find((item) => item.role === loginRole.value)?.title || '账号')

const redirectAfterLogin = () => {
  if (loginRole.value === 'ADMIN') router.push('/admin')
  else if (loginRole.value === 'CHECKER') router.push('/admin/checkin')
  else router.push(route.query.redirect || '/user')
}

const submitLogin = async () => {
  try {
    const profile = await user.login({ ...loginForm, roleCode: loginRole.value })
    if (profile.roleCode !== loginRole.value) {
      await user.logout()
      ElMessage.error('请选择正确的登录入口')
      return
    }
    ElMessage.success('登录成功')
    redirectAfterLogin()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const submitRegister = async () => {
  try {
    const profile = await user.register(registerForm)
    if (profile.roleCode !== 'USER') {
      await user.logout()
      ElMessage.error('普通注册只能创建用户账号')
      return
    }
    ElMessage.success('注册成功')
    router.push('/user')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<style scoped>
.role-entry {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.role-entry button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.role-entry button.active {
  border-color: #d9303e;
  color: #d9303e;
}

.role-entry span {
  color: #6b7280;
  font-size: 13px;
}
</style>

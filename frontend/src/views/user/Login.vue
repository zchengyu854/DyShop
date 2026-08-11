<script setup>
import { reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
})
const error = shallowRef('')
const submitting = shallowRef(false)

async function handleSubmit() {
  if (submitting.value) return
  error.value = ''
  submitting.value = true
  try {
    await userStore.login(form)
    // ch08.3：管理员账号登录 → 会话只写入后台 token（不污染 C 端），直接进后台
    if (userStore.isAdmin) {
      const adminStore = useAdminStore()
      adminStore.adopt(userStore.token, userStore.userInfo)
      userStore.logout()
      router.replace('/admin/dashboard')
      return
    }
    const q = route.query.redirect
    const redirect =
      typeof q === 'string' &&
      q.startsWith('/') &&
      !q.startsWith('//') &&
      q !== '/login' &&
      q !== '/register'
        ? q
        : ''
    router.replace(redirect || '/')
  } catch (e) {
    error.value = e.message || '登录失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="auth-card" @submit.prevent="handleSubmit">
      <h1 class="auth-title">dyshop</h1>
      <p class="auth-sub">登录你的账号</p>

      <input
        v-model.trim="form.username"
        class="auth-input"
        type="text"
        placeholder="用户名"
        autocomplete="username"
      />
      <input
        v-model="form.password"
        class="auth-input"
        type="password"
        placeholder="密码"
        autocomplete="current-password"
      />

      <p v-if="error" class="auth-error">{{ error }}</p>

      <button class="auth-btn" type="submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </button>

      <p class="auth-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
      <p class="auth-link admin-link">
        管理员？
        <router-link to="/admin/login">前往后台登录</router-link>
      </p>
    </form>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--bg-gray);
}
.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 36px;
  background: var(--bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-hover);
  text-align: center;
}
.auth-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.015em;
}
.auth-sub {
  margin: 8px 0 28px;
  font-size: 14px;
  color: var(--ink-secondary);
}
.auth-input {
  display: block;
  width: 100%;
  margin-bottom: 12px;
  padding: 11px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  outline: none;
  font-size: 14px;
  color: var(--ink);
  background: var(--bg);
  transition: border-color 0.2s;
}
.auth-input:focus {
  border-color: var(--blue);
}
.auth-input::placeholder {
  color: var(--ink-faint);
}
.auth-error {
  margin: 4px 0 12px;
  font-size: 13px;
  color: #ff3b30;
  text-align: left;
}
.auth-btn {
  width: 100%;
  padding: 11px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}
.auth-btn:hover {
  background: var(--blue-hover);
}
.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-link {
  margin: 16px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.admin-link {
  margin-top: 4px;
}
.auth-link a {
  color: var(--link);
}
.auth-link a:hover {
  text-decoration: underline;
}
</style>

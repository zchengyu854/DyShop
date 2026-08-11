<script setup>
import { reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})
const error = shallowRef('')
const submitting = shallowRef(false)

function validate() {
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(form.username)) {
    return '用户名需为 3~20 位字母、数字或下划线'
  }
  if (form.password.length < 6 || form.password.length > 20) {
    return '密码长度需为 6~20 位'
  }
  if (form.password !== form.confirmPassword) {
    return '两次输入的密码不一致'
  }
  return ''
}

async function handleSubmit() {
  if (submitting.value) return
  error.value = validate()
  if (error.value) return
  submitting.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
    })
    router.replace('/')
  } catch (e) {
    error.value = e.message || '注册失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="auth-card" @submit.prevent="handleSubmit">
      <h1 class="auth-title">dyshop</h1>
      <p class="auth-sub">创建你的账号</p>

      <input
        v-model.trim="form.username"
        class="auth-input"
        type="text"
        placeholder="用户名（3~20 位字母、数字或下划线）"
        autocomplete="username"
      />
      <input
        v-model.trim="form.nickname"
        class="auth-input"
        type="text"
        placeholder="昵称（可选）"
        autocomplete="nickname"
      />
      <input
        v-model="form.password"
        class="auth-input"
        type="password"
        placeholder="密码（6~20 位）"
        autocomplete="new-password"
      />
      <input
        v-model="form.confirmPassword"
        class="auth-input"
        type="password"
        placeholder="确认密码"
        autocomplete="new-password"
      />

      <p v-if="error" class="auth-error">{{ error }}</p>

      <button class="auth-btn" type="submit" :disabled="submitting">
        {{ submitting ? '注册中…' : '注册' }}
      </button>

      <p class="auth-link">
        已有账号？
        <router-link :to="{ path: '/login', query: { redirect: route.query.redirect || '/' } }">去登录</router-link>
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
  margin: 18px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
}
.auth-link a {
  color: var(--link);
}
.auth-link a:hover {
  text-decoration: underline;
}
</style>

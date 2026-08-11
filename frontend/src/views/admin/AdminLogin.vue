<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const submitting = ref(false)

// 从守卫带回来的提示：非后台权限 / 未登录
const denied = typeof route.query.denied === 'string' ? route.query.denied : ''

function safeRedirect() {
  const q = route.query.redirect
  return typeof q === 'string' && q.startsWith('/') && !q.startsWith('//') && q !== '/admin/login'
    ? q
    : '/admin/dashboard'
}

async function handleSubmit() {
  if (submitting.value) return
  error.value = ''
  submitting.value = true
  try {
    await adminStore.login(form)
    router.replace(safeRedirect())
  } catch (e) {
    // 非管理员账号：login 抛「无后台权限」错误，未写入任何 token，C 端会话不受影响
    error.value = e.message || '登录失败，请重试'
  } finally {
    submitting.value = false
  }
}

// 已有后台登录态 → 直接进后台；token 无效/非管理员 → 清掉留在登录页
onMounted(async () => {
  if (!adminStore.token) return
  try {
    await adminStore.fetchAdminInfo()
    if (adminStore.adminInfo?.role === 1) {
      router.replace('/admin/dashboard')
    } else {
      adminStore.logout()
    }
  } catch (e) {
    adminStore.logout()
  }
})
</script>

<template>
  <div class="admin-login-page">
    <form class="login-card" @submit.prevent="handleSubmit">
      <h1 class="brand">dyshop 管理后台</h1>
      <p class="sub">请使用管理员账号登录</p>

      <p v-if="denied === '1'" class="notice">当前账号无后台访问权限，请切换管理员账号登录</p>

      <input
        v-model.trim="form.username"
        class="input"
        type="text"
        placeholder="管理员用户名"
        autocomplete="username"
      />
      <input
        v-model="form.password"
        class="input"
        type="password"
        placeholder="密码"
        autocomplete="current-password"
      />

      <p v-if="error" class="err">{{ error }}</p>

      <button class="submit" type="submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录后台' }}
      </button>

      <router-link to="/" class="back">← 返回商城</router-link>
    </form>

    <p class="demo-hint">演示账号 admin / admin123</p>
  </div>
</template>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--bg-gray);
  gap: 16px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 36px;
  background: var(--bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-hover);
  display: flex;
  flex-direction: column;
}
.brand {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.015em;
  text-align: center;
}
.sub {
  margin: 8px 0 24px;
  font-size: 13px;
  color: var(--ink-secondary);
  text-align: center;
}
.notice {
  margin: 0 0 12px;
  padding: 10px 12px;
  font-size: 13px;
  color: #b25000;
  background: rgba(255, 149, 0, 0.12);
  border-radius: 8px;
  text-align: center;
}
.input {
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
.input:focus {
  border-color: var(--blue);
}
.err {
  margin: 0 0 12px;
  font-size: 13px;
  color: #ff3b30;
  text-align: center;
}
.submit {
  height: 44px;
  margin-top: 4px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.submit:hover {
  background: var(--blue-hover);
}
.back {
  display: block;
  margin-top: 16px;
  font-size: 13px;
  color: var(--ink-secondary);
  text-align: center;
  text-decoration: none;
}
.back:hover {
  color: var(--blue);
}
.demo-hint {
  margin: 0;
  font-size: 12px;
  color: var(--ink-faint);
}
</style>
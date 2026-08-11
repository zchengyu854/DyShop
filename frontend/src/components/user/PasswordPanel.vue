<script setup>
import { reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { updatePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const error = shallowRef('')
const submitting = shallowRef(false)

function validate() {
  if (!form.oldPassword) return '请输入原密码'
  if (form.newPassword.length < 6 || form.newPassword.length > 20) {
    return '新密码长度需为 6~20 位'
  }
  if (form.newPassword !== form.confirmPassword) {
    return '两次输入的新密码不一致'
  }
  if (form.newPassword === form.oldPassword) {
    return '新密码不能与原密码相同'
  }
  return ''
}

async function handleSubmit() {
  if (submitting.value) return
  error.value = validate()
  if (error.value) return
  submitting.value = true
  try {
    await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    // 修改成功后强制重新登录
    userStore.logout()
    router.replace({ path: '/login', query: { redirect: '/' } })
  } catch (e) {
    error.value = e.message || '修改失败，请重试'
    submitting.value = false
  }
}
</script>

<template>
  <section class="panel">
    <h2 class="panel-title">修改密码</h2>

    <div class="card">
      <p class="card-hint">修改成功后需要重新登录</p>

      <div class="form-row">
        <label class="form-label" for="old-password">原密码</label>
        <input id="old-password" v-model="form.oldPassword" class="form-input" type="password" autocomplete="current-password" placeholder="请输入原密码" />
      </div>
      <div class="form-row">
        <label class="form-label" for="new-password">新密码</label>
        <input id="new-password" v-model="form.newPassword" class="form-input" type="password" autocomplete="new-password" placeholder="6~20 位新密码" />
      </div>
      <div class="form-row">
        <label class="form-label" for="confirm-password">确认密码</label>
        <input id="confirm-password" v-model="form.confirmPassword" class="form-input" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <button class="save-btn" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中…' : '修改密码' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.panel-title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.card {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: 24px 28px 28px;
  box-shadow: var(--shadow-hover);
}
.card-hint {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.form-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.form-label {
  font-size: 14px;
  color: var(--ink);
}
.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  outline: none;
  font-size: 14px;
  color: var(--ink);
  background: var(--bg);
  transition: border-color 0.2s;
}
.form-input:focus {
  border-color: var(--blue);
}
.form-input::placeholder {
  color: var(--ink-faint);
}
.form-error {
  margin: 4px 0 12px;
  font-size: 13px;
  color: #ff3b30;
}
.save-btn {
  margin-top: 4px;
  padding: 10px 32px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.save-btn:hover {
  background: var(--blue-hover);
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

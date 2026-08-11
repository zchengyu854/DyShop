<script setup>
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const form = reactive({
  nickname: '',
  phone: '',
  email: '',
})
const saving = shallowRef(false)
const error = shallowRef('')
const success = shallowRef('')

const avatarChar = computed(() => {
  const name = form.nickname || userStore.userInfo?.username
  return name ? name.slice(0, 1) : '?'
})

onMounted(async () => {
  await userStore.fetchUserInfo()
  form.nickname = userStore.userInfo?.nickname || ''
  form.phone = userStore.userInfo?.phone || ''
  form.email = userStore.userInfo?.email || ''
})

async function handleSave() {
  if (saving.value) return
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: form.nickname.trim() || undefined,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
    })
    success.value = '已保存'
  } catch (e) {
    error.value = e.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="panel">
    <h2 class="panel-title">个人资料</h2>

    <div class="card">
      <div class="profile-head">
        <div class="avatar">{{ avatarChar }}</div>
        <div class="profile-meta">
          <p class="profile-name">{{ form.nickname || userStore.userInfo?.username }}</p>
          <p class="profile-sub">头像暂不支持自定义</p>
        </div>
      </div>

      <div class="form-row">
        <label class="form-label" for="nickname">昵称</label>
        <input id="nickname" v-model.trim="form.nickname" class="form-input" type="text" maxlength="20" placeholder="请输入昵称" />
      </div>
      <div class="form-row">
        <label class="form-label" for="phone">手机号</label>
        <input id="phone" v-model.trim="form.phone" class="form-input" type="text" maxlength="11" placeholder="请输入手机号" />
      </div>
      <div class="form-row">
        <label class="form-label" for="email">邮箱</label>
        <input id="email" v-model.trim="form.email" class="form-input" type="text" placeholder="请输入邮箱" />
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="success" class="form-success">{{ success }}</p>

      <button class="save-btn" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中…' : '保存更改' }}
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
  border-radius: var(--radius-card);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 0;
  border-bottom: 1px solid var(--border-line);
  margin-bottom: 24px;
}
.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6e6e73, #1d1d1f);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  flex-shrink: 0;
}
.profile-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.profile-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ink-secondary);
}
.form-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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
.form-success {
  margin: 4px 0 12px;
  font-size: 13px;
  color: #34c759;
}
.save-btn {
  margin-top: 4px;
  height: 44px;
  padding: 0 32px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--blue);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
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

<script setup>
import { toast } from '@/utils/toast'

const rows = [
  { label: '修改密码', desc: '定期更换密码更安全', to: '/user/password', action: 'link' },
  { label: '绑定微信', desc: '微信快捷登录与安全校验', action: 'todo' },
  { label: '绑定支付宝', desc: '支付安全校验', action: 'todo' },
  { label: '登录设备管理', desc: '查看与下线已登录设备', action: 'todo' },
]

function handleRow(row) {
  if (row.action === 'todo') toast.info('该功能开发中，敬请期待')
}
</script>

<template>
  <section class="security">
    <h3 class="card-title">安全与隐私</h3>
    <ul class="list">
      <li v-for="row in rows" :key="row.label" class="row">
        <router-link v-if="row.to" :to="row.to" class="row-body">
          <div class="row-text">
            <span class="row-label">{{ row.label }}</span>
            <span class="row-desc">{{ row.desc }}</span>
          </div>
          <span class="row-arrow">›</span>
        </router-link>
        <button v-else class="row-body" @click="handleRow(row)">
          <div class="row-text">
            <span class="row-label">{{ row.label }}</span>
            <span class="row-desc">{{ row.desc }}</span>
          </div>
          <span class="row-arrow">›</span>
        </button>
      </li>
    </ul>
    <div class="pwd-strength">
      <div class="strength-head">
        <span class="strength-label">密码安全等级</span>
        <span class="strength-text">良好</span>
      </div>
      <div class="strength-track">
        <div class="strength-fill" style="width: 60%" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.security {
  background: var(--bg);
  border-radius: var(--radius-card);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}
.card-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--ink);
}
.list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.row + .row {
  border-top: 1px solid var(--border-line);
}
.row-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 44px;
  padding: 8px 0;
  border: none;
  background: transparent;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
}
.row-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.row-label {
  font-size: 14px;
  font-weight: 500;
}
.row-desc {
  font-size: 12px;
  color: var(--ink-secondary);
}
.row-arrow {
  font-size: 20px;
  line-height: 1;
  color: var(--ink-faint);
}
.pwd-strength {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border-line);
}
.strength-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.strength-label {
  font-size: 13px;
  color: var(--ink-secondary);
}
.strength-text {
  font-size: 12px;
  font-weight: 600;
  color: #ff9500;
}
.strength-track {
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--bg-gray);
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  border-radius: var(--radius-full);
  background: #ff9500;
}
</style>

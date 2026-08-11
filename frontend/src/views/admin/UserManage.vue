<script setup>
import { onMounted, ref } from 'vue'
import { changeUserRole, changeUserStatus } from '@/api/admin/user'
import { fetchMemberLevels, fetchMemberUsers, updateMemberLevel } from '@/api/admin/member'
import { useAdminStore } from '@/stores/admin'
import { toast } from '@/utils/toast'

const adminStore = useAdminStore()

// 用户管理与会员管理合并为同一模块（两个标签页）
const tab = ref('users')

// ---------- 用户列表 ----------
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

const roleTarget = ref(null) // { id, username, to }
const statusTarget = ref(null) // { id, username, to }

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

function fmtMoney(v) {
  if (v == null) return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load({ silent = false } = {}) {
  if (!silent) loading.value = true
  try {
    const params = { page: page.value, size }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await fetchMemberUsers(params)
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '用户加载失败')
  } finally {
    if (!silent) loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  load()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

function isSelf(row) {
  return row.id === adminStore.adminInfo?.id
}

function levelClass(code) {
  return String(code || 'normal').toLowerCase()
}

async function doChangeRole() {
  const target = roleTarget.value
  roleTarget.value = null
  try {
    await changeUserRole(target.id, target.to)
    toast.success(target.to === 1 ? '已设为管理员' : '已取消管理员')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

async function doChangeStatus() {
  const target = statusTarget.value
  statusTarget.value = null
  try {
    await changeUserStatus(target.id, target.to)
    toast.success(target.to === 1 ? '已禁用' : '已启用')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

// ---------- 会员等级配置 ----------
const levels = ref([])
const levelLoading = ref(false)
const saving = ref(false)

async function loadLevels() {
  levelLoading.value = true
  try {
    levels.value = await fetchMemberLevels()
  } catch (e) {
    toast.error(e.message || '等级配置加载失败')
  } finally {
    levelLoading.value = false
  }
}

function fmtNum(v) {
  if (v == null) return ''
  return String(Number(v).toFixed(Number(v) % 1 === 0 ? 0 : 2))
}

async function saveLevel(row) {
  const threshold = Number(row.threshold)
  const discountRate = Number(row.discountRate)
  const pointRate = Number(row.pointRate)
  if (!Number.isFinite(threshold) || threshold < 0) return toast.error('门槛必须 ≥ 0')
  if (!Number.isFinite(discountRate) || discountRate <= 0 || discountRate > 1) {
    return toast.error('折扣率必须在 0~1 之间')
  }
  if (!Number.isFinite(pointRate) || pointRate <= 0) return toast.error('积分倍率必须 > 0')
  saving.value = true
  try {
    await updateMemberLevel(row.id, { threshold, discountRate, pointRate })
    toast.success('已保存')
    loadLevels()
  } catch (e) {
    toast.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function switchTab(t) {
  tab.value = t
  if (t === 'users') load()
  else loadLevels()
}

onMounted(() => {
  load()
  loadLevels()
})
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">用户管理</h1>
    </div>

    <div class="tabbar">
      <button class="tab" :class="{ active: tab === 'users' }" @click="switchTab('users')">
        用户列表
      </button>
      <button class="tab" :class="{ active: tab === 'levels' }" @click="switchTab('levels')">
        会员等级
      </button>
    </div>

    <!-- 用户列表 -->
    <template v-if="tab === 'users'">
      <div class="filters">
        <input
          v-model="keyword"
          class="input"
          type="text"
          placeholder="用户名 / 昵称 / 手机号"
          @keyup.enter="search"
        />
        <button class="op search-btn" @click="search">搜索</button>
        <button v-if="keyword" class="op" @click="keyword = ''; search()">重置</button>
      </div>

      <div v-if="loading" class="hint">加载中…</div>
      <div v-else-if="records.length === 0" class="hint">暂无用户</div>

      <table v-else class="table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>手机号</th>
            <th>角色</th>
            <th>状态</th>
            <th>会员等级</th>
            <th>近12月消费</th>
            <th>累计消费</th>
            <th>积分</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in records" :key="u.id">
            <td class="mono">{{ u.username }}</td>
            <td>{{ u.nickname || '-' }}</td>
            <td class="mono">{{ u.phone || '-' }}</td>
            <td>
              <span class="status" :class="u.role === 1 ? 'admin' : 'buyer'">
                {{ u.role === 1 ? '管理员' : '买家' }}
              </span>
            </td>
            <td>
              <span class="status" :class="u.status === 0 ? 'on' : 'warn'">
                {{ u.status === 0 ? '正常' : '已禁用' }}
              </span>
            </td>
            <td>
              <span v-if="u.level" class="lv" :class="levelClass(u.level.code)">{{ u.level.name }}</span>
              <span v-else class="lv normal">普通</span>
            </td>
            <td class="mono">{{ fmtMoney(u.annualConsumption) }}</td>
            <td class="mono">{{ fmtMoney(u.totalConsumption) }}</td>
            <td class="mono">{{ u.points ?? 0 }}</td>
            <td class="mono">{{ fmtTime(u.createTime) }}</td>
            <td class="ops">
              <template v-if="!isSelf(u)">
                <button
                  v-if="u.role === 0"
                  class="op primary"
                  @click="roleTarget = { id: u.id, username: u.username, to: 1 }"
                >
                  设管理员
                </button>
                <button
                  v-else
                  class="op"
                  @click="roleTarget = { id: u.id, username: u.username, to: 0 }"
                >
                  取消管理员
                </button>
                <button
                  v-if="u.status === 0"
                  class="op danger"
                  @click="statusTarget = { id: u.id, username: u.username, to: 1 }"
                >
                  禁用
                </button>
                <button v-else class="op primary" @click="statusTarget = { id: u.id, username: u.username, to: 0 }">
                  启用
                </button>
              </template>
              <template v-else>
                <span class="self">当前账号</span>
              </template>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="records.length" class="pager">
        <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 人）</span>
        <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
      </div>
    </template>

    <!-- 会员等级配置 -->
    <template v-else>
      <p class="tip">
        等级按「近 12 个月已支付消费」实时判定（支付即计入，退款从消费中扣减），修改门槛/折扣/倍率后立即生效（NORMAL 为无权益档，勿调低折扣率）。
      </p>
      <div v-if="levelLoading" class="hint">加载中…</div>
      <div v-else-if="levels.length === 0" class="hint">暂无等级配置</div>
      <table v-else class="table" style="max-width: 52rem">
        <thead>
          <tr>
            <th>等级</th>
            <th>消费门槛(¥)</th>
            <th>折扣率</th>
            <th>积分倍率</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in levels" :key="l.id">
            <td>
              <strong>{{ l.name }}</strong>
              <span class="code">{{ l.code }}</span>
            </td>
            <td><input v-model="l.threshold" class="input" type="number" min="0" step="0.01" /></td>
            <td><input v-model="l.discountRate" class="input" type="number" min="0.01" max="1" step="0.01" /></td>
            <td><input v-model="l.pointRate" class="input" type="number" min="0.1" step="0.1" /></td>
            <td>
              <button class="op primary" :disabled="saving" @click="saveLevel(l)">保存</button>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <!-- 角色确认 -->
    <div v-if="roleTarget" class="mask" @click.self="roleTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">{{ roleTarget.to === 1 ? '设为管理员' : '取消管理员' }}</h3>
        <p class="confirm-desc">
          {{ roleTarget.to === 1 ? '将「' + roleTarget.username + '」设为管理员后，其登录将进入后台管理，确定吗？' : '取消后「' + roleTarget.username + '」将不再拥有后台访问权限，确定吗？' }}
        </p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="roleTarget = null">再想想</button>
          <button class="confirm-btn primary" @click="doChangeRole">确定</button>
        </div>
      </div>
    </div>

    <!-- 禁用确认 -->
    <div v-if="statusTarget" class="mask" @click.self="statusTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">禁用用户</h3>
        <p class="confirm-desc">禁用后「{{ statusTarget.username }}」将立即无法登录与访问，确定吗？</p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="statusTarget = null">再想想</button>
          <button class="confirm-btn danger" @click="doChangeStatus">确定禁用</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 共享样式见 assets/admin.css（.admin-page 包装类），此处仅保留页面独有样式 */
.tabbar {
  display: flex;
  gap: 0.5rem;
}
.tab {
  height: 2.125rem;
  padding: 0 1.25rem;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 0.875rem;
  cursor: pointer;
}
.tab:hover {
  background: var(--bg-gray);
}
.tab.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.tip {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.code {
  margin-left: 6px;
  font-size: 11px;
  color: var(--ink-faint);
}
.lv {
  display: inline-block;
  padding: 0.125rem 0.625rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
}
.lv.normal {
  color: #86868b;
  background: rgba(134, 134, 139, 0.12);
}
.lv.silver {
  color: #6e6e73;
  background: rgba(120, 120, 128, 0.22);
}
.lv.gold {
  color: #b8860b;
  background: rgba(184, 134, 11, 0.12);
}
.lv.diamond {
  color: #0071e3;
  background: rgba(0, 113, 227, 0.12);
}
.self {
  font-size: 0.75rem;
  color: var(--ink-faint);
}
</style>
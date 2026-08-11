<script setup>
import { onMounted, ref } from 'vue'
import {
  changeCouponTemplateStatus,
  createCouponTemplate,
  deleteCouponTemplate,
  fetchCouponTemplates,
  grantCoupon,
  updateCouponTemplate,
} from '@/api/admin/coupon'
import { fetchAdminUsers } from '@/api/admin/user'
import { toast } from '@/utils/toast'

const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)

// 新建/编辑抽屉
const drawerOpen = ref(false)
const editing = ref(null) // null=新建
const form = ref({})
const submitting = ref(false)

// 发放弹窗
const grantTarget = ref(null)
const grantForm = ref({ target: 'all', userIds: [], keyword: '', list: [], listPage: 1, listTotal: 0 })

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

function fmtList(json) {
  if (!json) return '-'
  try {
    return JSON.parse(json).join('、')
  } catch {
    return json
  }
}

const defaultForm = () => ({
  name: '',
  type: 'REDUCE',
  minAmount: 0,
  discountAmount: '',
  scope: 'ALL',
  categoryIds: '',
  productIds: '',
  allowStack: 0,
  issueType: 'CENTER',
  validType: 'FIXED',
  startAt: '',
  endAt: '',
  validDays: 0,
  totalQuantity: -1,
  perUser: 1,
})

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (statusFilter.value) params.status = Number(statusFilter.value)
    const data = await fetchCouponTemplates(params)
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function goPage(p) {
  if (p < 1 || p > totalPages()) return
  page.value = p
  load()
}

const totalPages = () => Math.max(1, Math.ceil(total.value / size))

function openCreate() {
  editing.value = null
  form.value = defaultForm()
  drawerOpen.value = true
}

function openEdit(tpl) {
  editing.value = tpl
  form.value = {
    name: tpl.name,
    type: tpl.type,
    minAmount: Number(tpl.minAmount),
    discountAmount: Number(tpl.discountAmount),
    scope: tpl.scope,
    categoryIds: tpl.categoryIds ? JSON.parse(tpl.categoryIds).join(',') : '',
    productIds: tpl.productIds ? JSON.parse(tpl.productIds).join(',') : '',
    allowStack: tpl.allowStack,
    issueType: tpl.issueType,
    validType: tpl.validType,
    startAt: tpl.startAt ? String(tpl.startAt).slice(0, 16) : '',
    endAt: tpl.endAt ? String(tpl.endAt).slice(0, 16) : '',
    validDays: tpl.validDays,
    totalQuantity: tpl.totalQuantity,
    perUser: tpl.perUser,
  }
  drawerOpen.value = true
}

/** 已发放模板仅允许改名称（后端同规则），其余字段禁用 */
const lockedFields = () => editing.value != null && Number(editing.value.issuedCount) > 0

function parseIds(text) {
  const parts = String(text || '')
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter((s) => /^\d+$/.test(s))
  return parts.length ? JSON.stringify(parts.map(Number)) : null
}

async function submit() {
  if (submitting.value) return
  if (!form.value.name.trim()) return toast.error('请输入模板名称')
  if (!(Number(form.value.discountAmount) > 0)) return toast.error('立减金额必须大于 0')
  if (form.value.scope === 'LIMITED' && !parseIds(form.value.categoryIds) && !parseIds(form.value.productIds)) {
    return toast.error('限定范围券至少需指定分类或商品一项')
  }
  submitting.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      type: 'REDUCE',
      minAmount: Number(form.value.minAmount) || 0,
      discountAmount: Number(form.value.discountAmount),
      scope: form.value.scope,
      categoryIds: parseIds(form.value.categoryIds),
      productIds: parseIds(form.value.productIds),
      allowStack: form.value.allowStack ? 1 : 0,
      issueType: form.value.issueType,
      validType: form.value.validType,
      startAt: form.value.validType === 'FIXED' && form.value.startAt ? form.value.startAt : null,
      endAt: form.value.validType === 'FIXED' && form.value.endAt ? form.value.endAt : null,
      validDays: Number(form.value.validDays) || 0,
      totalQuantity: Number(form.value.totalQuantity) || -1,
      perUser: 1,
    }
    if (editing.value) {
      await updateCouponTemplate(editing.value.id, payload)
      toast.success('已保存')
    } else {
      await createCouponTemplate(payload)
      toast.success('创建成功')
    }
    drawerOpen.value = false
    load()
  } catch (e) {
    toast.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(tpl) {
  try {
    await changeCouponTemplateStatus(tpl.id, tpl.status === 1 ? 0 : 1)
    toast.success(tpl.status === 1 ? '已停用' : '已启用')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

async function removeTemplate(tpl) {
  if (!window.confirm(`确定删除模板「${tpl.name}」？仅停用状态可删除。`)) return
  try {
    await deleteCouponTemplate(tpl.id)
    toast.success('已删除')
    load()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

// ---------- 发放 ----------

function openGrant(tpl) {
  grantTarget.value = tpl
  grantForm.value = { target: 'all', userIds: [], keyword: '', list: [], listPage: 1, listTotal: 0 }
  loadGrantUsers()
}

async function loadGrantUsers() {
  const f = grantForm.value
  try {
    const params = { page: f.listPage, size: 10 }
    if (f.keyword.trim()) params.keyword = f.keyword.trim()
    const data = await fetchAdminUsers(params)
    f.list = data.records
    f.listTotal = Number(data.total)
  } catch (e) {
    toast.error(e.message || '用户加载失败')
  }
}

function toggleUser(id) {
  const ids = grantForm.value.userIds
  const idx = ids.indexOf(id)
  if (idx >= 0) ids.splice(idx, 1)
  else ids.push(id)
}

async function doGrant() {
  const f = grantForm.value
  if (f.target === 'manual' && !f.userIds.length) return toast.error('请选择要发放的用户')
  try {
    const res = await grantCoupon({
      templateId: grantTarget.value.id,
      target: f.target,
      userIds: f.target === 'manual' ? f.userIds : undefined,
    })
    toast.success(`发放成功：新增 ${res.granted} 张，跳过 ${res.skipped} 张`)
    grantTarget.value = null
    load()
  } catch (e) {
    toast.error(e.message || '发放失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">优惠券模板</h1>
      <button class="op primary" @click="openCreate">＋ 新建模板</button>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="input" type="text" placeholder="模板名称" @keyup.enter="search" />
      <select v-model="statusFilter" class="input select" @change="search">
        <option value="">全部状态</option>
        <option value="1">启用</option>
        <option value="0">停用</option>
      </select>
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword || statusFilter" class="op" @click="keyword = ''; statusFilter = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无优惠券模板</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>面额</th>
          <th>门槛</th>
          <th>范围</th>
          <th>渠道</th>
          <th>总量/已发</th>
          <th>有效期</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in records" :key="t.id">
          <td class="mono">{{ t.id }}</td>
          <td>
            {{ t.name }}
            <span v-if="t.allowStack === 1" class="stack-tag" title="可与会员折扣叠加">叠加</span>
          </td>
          <td class="mono">-¥{{ Number(t.discountAmount).toFixed(2) }}</td>
          <td class="mono">{{ Number(t.minAmount) > 0 ? '满 ¥' + Number(t.minAmount) : '无门槛' }}</td>
          <td>
            <span v-if="t.scope === 'ALL'">全场</span>
            <span v-else>分类[{{ fmtList(t.categoryIds) }}] 商品[{{ fmtList(t.productIds) }}]</span>
          </td>
          <td>{{ t.issueType === 'CENTER' ? '可领取' : '仅发放' }}</td>
          <td class="mono">{{ t.totalQuantity === -1 ? '不限' : t.totalQuantity }} / {{ t.issuedCount }}</td>
          <td class="mono">
            {{ t.validType === 'FIXED' ? (t.startAt ? fmtTime(t.startAt) : '长期') + ' ~ ' + (t.endAt ? fmtTime(t.endAt) : '长期') : '领取后 ' + t.validDays + ' 天' }}
          </td>
          <td>
            <span class="status" :style="t.status === 1 ? 'color:#34c759;background:#34c7591a' : 'color:#86868b;background:#86868b1a'">
              {{ t.status === 1 ? '启用' : '停用' }}
            </span>
          </td>
          <td class="ops">
            <button class="op" @click="openGrant(t)">发放</button>
            <button class="op" @click="openEdit(t)">编辑</button>
            <button class="op" @click="toggleStatus(t)">{{ t.status === 1 ? '停用' : '启用' }}</button>
            <button class="op danger" @click="removeTemplate(t)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 个模板）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 新建/编辑抽屉 -->
    <div v-if="drawerOpen" class="mask" @click.self="drawerOpen = false">
      <aside class="drawer form-drawer">
        <div class="drawer-head">
          <h2>{{ editing ? '编辑模板' : '新建模板' }}</h2>
          <button class="close" @click="drawerOpen = false">×</button>
        </div>
        <p v-if="lockedFields()" class="lock-hint">该券已发放，仅可修改名称（金额/范围/有效期仅对新券生效）</p>

        <div class="form">
          <label class="field">
            <span class="label">模板名称</span>
            <input v-model="form.name" class="input" type="text" placeholder="如：满 300 减 30" maxlength="64" />
          </label>
          <div class="field-row">
            <label class="field">
              <span class="label">面额（元）</span>
              <input v-model="form.discountAmount" class="input" type="number" min="0.01" step="0.01" :disabled="lockedFields()" />
            </label>
            <label class="field">
              <span class="label">门槛（元，0=无门槛）</span>
              <input v-model="form.minAmount" class="input" type="number" min="0" step="0.01" :disabled="lockedFields()" />
            </label>
          </div>

          <div class="field-row">
            <label class="field">
              <span class="label">适用范围</span>
              <select v-model="form.scope" class="input select" :disabled="lockedFields()">
                <option value="ALL">全场</option>
                <option value="LIMITED">指定分类/商品</option>
              </select>
            </label>
            <label class="field">
              <span class="label">发放渠道</span>
              <select v-model="form.issueType" class="input select" :disabled="lockedFields()">
                <option value="CENTER">领券中心可领取</option>
                <option value="MANUAL_ONLY">仅后台发放</option>
              </select>
            </label>
          </div>

          <template v-if="form.scope === 'LIMITED'">
            <label class="field">
              <span class="label">指定分类 ID（逗号分隔，与商品并集生效）</span>
              <input v-model="form.categoryIds" class="input" type="text" placeholder="如：1,2" :disabled="lockedFields()" />
            </label>
            <label class="field">
              <span class="label">指定商品 ID（逗号分隔，至少填一项）</span>
              <input v-model="form.productIds" class="input" type="text" placeholder="如：4,7" :disabled="lockedFields()" />
            </label>
          </template>

          <div class="field-row">
            <label class="field">
              <span class="label">有效期类型</span>
              <select v-model="form.validType" class="input select" :disabled="lockedFields()">
                <option value="FIXED">固定起止</option>
                <option value="AFTER_DAYS">领取后 N 天</option>
              </select>
            </label>
            <label class="field">
              <span class="label">总量（-1=不限）</span>
              <input v-model="form.totalQuantity" class="input" type="number" :disabled="lockedFields()" />
            </label>
          </div>

          <template v-if="form.validType === 'FIXED'">
            <div class="field-row">
              <label class="field">
                <span class="label">开始时间</span>
                <input v-model="form.startAt" class="input" type="datetime-local" :disabled="lockedFields()" />
              </label>
              <label class="field">
                <span class="label">结束时间（留空=长期）</span>
                <input v-model="form.endAt" class="input" type="datetime-local" :disabled="lockedFields()" />
              </label>
            </div>
          </template>
          <label v-else class="field">
            <span class="label">有效天数（0=长期）</span>
            <input v-model="form.validDays" class="input" type="number" min="0" :disabled="lockedFields()" />
          </label>

          <label class="field check">
            <input v-model="form.allowStack" type="checkbox" :disabled="lockedFields()" />
            <span>允许与会员折扣叠加（默认互斥）</span>
          </label>
        </div>

        <button class="ship-btn" :disabled="submitting" @click="submit">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
      </aside>
    </div>

    <!-- 发放弹窗 -->
    <div v-if="grantTarget" class="mask" @click.self="grantTarget = null">
      <div class="drawer grant-drawer">
        <div class="drawer-head">
          <h2>发放「{{ grantTarget.name }}」</h2>
          <button class="close" @click="grantTarget = null">×</button>
        </div>
        <div class="form">
          <div class="field-row">
            <label class="field radio-line">
              <input v-model="grantForm.target" type="radio" value="all" />
              <span>全员发放</span>
            </label>
            <label class="field radio-line">
              <input v-model="grantForm.target" type="radio" value="manual" />
              <span>指定用户</span>
            </label>
          </div>
          <template v-if="grantForm.target === 'manual'">
            <div class="grant-search">
              <input v-model="grantForm.keyword" class="input" type="text" placeholder="用户名 / 手机号"
                @keyup.enter="grantForm.listPage = 1; loadGrantUsers()" />
              <button class="op" @click="grantForm.listPage = 1; loadGrantUsers()">搜索</button>
            </div>
            <div class="user-list">
              <label v-for="u in grantForm.list" :key="u.id" class="user-item">
                <input type="checkbox" :checked="grantForm.userIds.includes(u.id)" @change="toggleUser(u.id)" />
                <span>{{ u.username }}</span>
                <span class="user-phone mono">{{ u.phone || '-' }}</span>
              </label>
              <p v-if="!grantForm.list.length" class="hint">无匹配用户</p>
            </div>
            <p class="grant-page">已选 {{ grantForm.userIds.length }} 人 · 第 {{ grantForm.listPage }} 页</p>
          </template>
        </div>
        <button class="ship-btn" @click="doGrant">确认发放</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  margin: 0;
}
.stack-tag {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  color: #0071e3;
  background: rgba(0, 113, 227, 0.08);
}
.lock-hint {
  margin: 0 0 12px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #ff9500;
  background: rgba(255, 149, 0, 0.1);
}
.form-drawer {
  width: 30rem;
  overflow-y: auto;
}
.grant-drawer {
  width: 26rem;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
.field-row {
  display: flex;
  gap: 12px;
}
.label {
  font-size: 12px;
  color: var(--ink-secondary);
}
.select {
  width: 100%;
}
.field.check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.field.radio-line {
  flex-direction: row;
  align-items: center;
  gap: 6px;
}
.grant-search {
  display: flex;
  gap: 8px;
}
.user-list {
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid var(--border-line);
  border-radius: 8px;
  padding: 4px 8px;
}
.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  cursor: pointer;
}
.user-phone {
  margin-left: auto;
  color: var(--ink-faint);
  font-size: 12px;
}
.grant-page {
  margin: 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.ship-btn {
  width: 100%;
  height: 2.625rem;
  margin-top: 1rem;
  border: none;
  border-radius: 0.625rem;
  background: var(--blue);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
.ship-btn:disabled {
  opacity: 0.6;
}
.op.danger {
  color: #e5484d;
}
</style>

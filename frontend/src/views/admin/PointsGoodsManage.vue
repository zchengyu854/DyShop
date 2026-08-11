<script setup>
import { onMounted, ref } from 'vue'
import {
  changePointsGoodsStatus,
  createPointsGoods,
  deletePointsGoods,
  fetchPointsGoods,
  updatePointsGoods,
} from '@/api/admin/points'
import { fetchCouponTemplates } from '@/api/admin/coupon'
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

// 券模板下拉（仅 COUPON 类需要）
const templates = ref([])

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const defaultForm = () => ({
  name: '',
  coverImage: '',
  description: '',
  goodsType: 'COUPON',
  pointCost: '',
  stock: -1,
  limitPerUser: 0,
  couponTemplateId: null,
  status: 1,
  sort: 0,
})

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (statusFilter.value) params.status = Number(statusFilter.value)
    const data = await fetchPointsGoods(params)
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

async function loadTemplates() {
  try {
    const data = await fetchCouponTemplates({ page: 1, size: 100, status: 1 })
    templates.value = data.records
  } catch {
    templates.value = []
  }
}

function openCreate() {
  editing.value = null
  form.value = defaultForm()
  drawerOpen.value = true
}

function openEdit(g) {
  editing.value = g
  form.value = {
    name: g.name,
    coverImage: g.coverImage || '',
    description: g.description || '',
    goodsType: g.goodsType,
    pointCost: g.pointCost,
    stock: g.stock,
    limitPerUser: g.limitPerUser,
    couponTemplateId: g.couponTemplateId ?? null,
    status: g.status,
    sort: g.sort,
  }
  drawerOpen.value = true
}

async function submit() {
  if (submitting.value) return
  if (!form.value.name.trim()) return toast.error('请输入商品名称')
  if (!(Number(form.value.pointCost) > 0)) return toast.error('积分价必须大于 0')
  if (Number(form.value.stock) === 0) return toast.error('库存不能为 0（不限请用 -1）')
  if (form.value.goodsType === 'COUPON' && !form.value.couponTemplateId) {
    return toast.error('COUPON 商品必须关联一个启用中的券模板')
  }
  submitting.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      coverImage: form.value.coverImage.trim() || null,
      description: form.value.description.trim() || null,
      goodsType: form.value.goodsType,
      pointCost: Number(form.value.pointCost),
      stock: Number(form.value.stock) || -1,
      limitPerUser: Number(form.value.limitPerUser) || 0,
      couponTemplateId: form.value.goodsType === 'COUPON' ? form.value.couponTemplateId : null,
      status: form.value.status ? 1 : 0,
      sort: Number(form.value.sort) || 0,
    }
    if (editing.value) {
      await updatePointsGoods(editing.value.id, payload)
      toast.success('已保存')
    } else {
      await createPointsGoods(payload)
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

async function toggleStatus(g) {
  try {
    await changePointsGoodsStatus(g.id, g.status === 1 ? 0 : 1)
    toast.success(g.status === 1 ? '已下架' : '已上架')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

async function removeGoods(g) {
  if (!window.confirm(`确定删除「${g.name}」？仅下架商品可删除。`)) return
  try {
    await deletePointsGoods(g.id)
    toast.success('已删除')
    load()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

onMounted(() => {
  load()
  loadTemplates()
})
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">积分商城商品</h1>
      <router-link class="op link-op" to="/admin/points-exchanges">兑换记录</router-link>
      <button class="op primary" @click="openCreate">＋ 新建商品</button>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="input" type="text" placeholder="商品名称" @keyup.enter="search" />
      <select v-model="statusFilter" class="input select" @change="search">
        <option value="">全部状态</option>
        <option value="1">上架</option>
        <option value="0">下架</option>
      </select>
      <button class="op" @click="search">搜索</button>
      <button v-if="keyword || statusFilter" class="op" @click="keyword = ''; statusFilter = ''; search()">重置</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无积分商品</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>类型</th>
          <th>积分价</th>
          <th>库存</th>
          <th>限兑</th>
          <th>关联券模板</th>
          <th>排序</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="g in records" :key="g.id">
          <td class="mono">{{ g.id }}</td>
          <td>{{ g.name }}</td>
          <td>
            <span class="type-tag" :class="g.goodsType === 'COUPON' ? 't-coupon' : 't-code'">
              {{ g.goodsType === 'COUPON' ? '优惠券' : '兑换码' }}
            </span>
          </td>
          <td class="mono">{{ g.pointCost }}</td>
          <td class="mono">{{ g.stock === -1 ? '不限' : g.stock }}</td>
          <td class="mono">{{ g.limitPerUser > 0 ? g.limitPerUser : '不限' }}</td>
          <td>{{ g.couponTemplateName || '-' }}</td>
          <td class="mono">{{ g.sort }}</td>
          <td>
            <span class="status" :style="g.status === 1 ? 'color:#34c759;background:#34c7591a' : 'color:#86868b;background:#86868b1a'">
              {{ g.status === 1 ? '上架' : '下架' }}
            </span>
          </td>
          <td class="ops">
            <button class="op" @click="openEdit(g)">编辑</button>
            <button class="op" @click="toggleStatus(g)">{{ g.status === 1 ? '下架' : '上架' }}</button>
            <button class="op danger" @click="removeGoods(g)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 件商品）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 新建/编辑抽屉 -->
    <div v-if="drawerOpen" class="mask" @click.self="drawerOpen = false">
      <aside class="drawer form-drawer">
        <div class="drawer-head">
          <h2>{{ editing ? '编辑商品' : '新建商品' }}</h2>
          <button class="close" @click="drawerOpen = false">×</button>
        </div>
        <p v-if="editing && editing.goodsType === 'COUPON'" class="lock-hint">
          已产生兑换记录的商品不可修改关联券模板（后端同样校验）
        </p>

        <div class="form">
          <label class="field">
            <span class="label">商品名称</span>
            <input v-model="form.name" class="input" type="text" placeholder="如：满 30 减 5 全场券" maxlength="64" />
          </label>
          <div class="field-row">
            <label class="field">
              <span class="label">商品类型</span>
              <select v-model="form.goodsType" class="input select" @change="form.couponTemplateId = null">
                <option value="COUPON">优惠券（发券到券包）</option>
                <option value="CODE">兑换码</option>
              </select>
            </label>
            <label class="field">
              <span class="label">积分价（>0）</span>
              <input v-model="form.pointCost" class="input" type="number" min="1" step="1" />
            </label>
          </div>
          <div class="field-row">
            <label class="field">
              <span class="label">库存（-1=不限）</span>
              <input v-model="form.stock" class="input" type="number" />
            </label>
            <label class="field">
              <span class="label">每人限兑（0=不限）</span>
              <input v-model="form.limitPerUser" class="input" type="number" min="0" />
            </label>
          </div>

          <label v-if="form.goodsType === 'COUPON'" class="field">
            <span class="label">关联券模板（仅启用中模板可选）</span>
            <select v-model="form.couponTemplateId" class="input select">
              <option :value="null" disabled>请选择券模板</option>
              <option v-for="t in templates" :key="t.id" :value="t.id">{{ t.name }}（-¥{{ Number(t.discountAmount).toFixed(0) }}）</option>
            </select>
            <span v-if="templates.length === 0" class="field-hint">暂无启用中的券模板，请先在「优惠券管理」创建</span>
          </label>

          <div class="field-row">
            <label class="field">
              <span class="label">封面图 URL</span>
              <input v-model="form.coverImage" class="input" type="text" placeholder="https://…（可留空）" />
            </label>
            <label class="field">
              <span class="label">排序（小在前）</span>
              <input v-model="form.sort" class="input" type="number" />
            </label>
          </div>
          <div class="field-row">
            <label class="field">
              <span class="label">状态</span>
              <select v-model="form.status" class="input select">
                <option :value="1">上架</option>
                <option :value="0">下架</option>
              </select>
            </label>
          </div>
          <label class="field">
            <span class="label">商品描述</span>
            <textarea v-model="form.description" class="input textarea" rows="3" placeholder="兑换后获得…" />
          </label>
        </div>

        <button class="ship-btn" :disabled="submitting" @click="submit">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.title {
  margin: 0;
  margin-right: auto;
}
.link-op {
  text-decoration: none;
}
.type-tag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
}
.t-coupon {
  color: #0071e3;
  background: rgba(0, 113, 227, 0.08);
}
.t-code {
  color: #ff9500;
  background: rgba(255, 149, 0, 0.12);
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
.textarea {
  resize: vertical;
  font-family: inherit;
}
.field-hint {
  font-size: 11px;
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
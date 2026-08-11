<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  changeProductStatus,
  createAdminProduct,
  deleteAdminProduct,
  fetchAdminProduct,
  fetchAdminProducts,
  updateAdminProduct,
} from '@/api/admin/product'
import { fetchAdminCategories } from '@/api/admin/category'
import { toast } from '@/utils/toast'

const keyword = ref('')
const categoryId = ref('')
const statusFilter = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref([])
const loading = ref(false)
const categories = ref([])

const drawer = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive(emptyForm())
const specOpen = ref(false)

const statusTarget = ref(null) // { id, name, to }
const deleteTarget = ref(null)

// SKU 库存/价格表格编辑器：与 form.skus JSON 双向同步（表格为便捷编辑，JSON 为底层真源）
const skuRows = ref([])

function parseSpecsJson() {
  try {
    const arr = JSON.parse(form.specs || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function parseSkusJson() {
  try {
    const arr = JSON.parse(form.skus || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function syncSkuRows() {
  skuRows.value = parseSkusJson()
}

function skuRowLabel(row) {
  const dims = parseSpecsJson()
  const specs = row.specs || {}
  if (dims.length) {
    return dims.map((d) => `${d.name}:${specs[d.name] ?? '-'}`).join(' · ')
  }
  const entries = Object.entries(specs)
  return entries.length ? entries.map(([k, v]) => `${k}:${v}`).join(' · ') : '（未指定规格）'
}

function normNum(v) {
  if (v === '' || v == null) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

// 表格变更 → 写回 skus JSON（数值规范化，保证后端可解析）
function rebuildSkusJson() {
  form.skus = JSON.stringify(
    skuRows.value.map((r) => ({
      id: Number(r.id) || 0,
      specs: r.specs || {},
      price: normNum(r.price),
      originalPrice: normNum(r.originalPrice),
      stock: normNum(r.stock) ?? 0,
      image: r.image ?? null,
    })),
    null,
    2
  )
}

function onSkuInput() {
  rebuildSkusJson()
}

function addSkuRow() {
  const maxId = skuRows.value.reduce((m, r) => Math.max(m, Number(r.id) || 0), 0)
  skuRows.value = [...skuRows.value, { id: maxId + 1, specs: {}, price: null, originalPrice: null, stock: 0 }]
  rebuildSkusJson()
}

function removeSkuRow(i) {
  skuRows.value = skuRows.value.filter((_, idx) => idx !== i)
  rebuildSkusJson()
}

function skuTotalStock() {
  return skuRows.value.reduce((s, r) => s + (Number(r.stock) || 0), 0)
}

function emptyForm() {
  return {
    categoryId: null,
    name: '',
    subtitle: '',
    mainImage: '',
    images: '',
    detail: '',
    price: '',
    originalPrice: '',
    stock: 0,
    status: 0,
    specs: '',
    skus: '',
  }
}

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const hasSpecs = () => {
  const s = (form.specs || '').trim()
  const k = (form.skus || '').trim()
  return s !== '' || k !== ''
}

async function loadCategories() {
  categories.value = await fetchAdminCategories()
}

async function load({ silent = false } = {}) {
  if (!silent) loading.value = true
  try {
    const params = { page: page.value, size }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (categoryId.value) params.categoryId = categoryId.value
    if (statusFilter.value !== '') params.status = Number(statusFilter.value)
    const data = await fetchAdminProducts(params)
    records.value = data.records
    total.value = Number(data.total)
  } catch (e) {
    toast.error(e.message || '商品加载失败')
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

function openCreate() {
  editingId.value = null
  Object.assign(form, { ...emptyForm(), status: 0 })
  specOpen.value = false
  skuRows.value = []
  drawer.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  try {
    const d = await fetchAdminProduct(row.id)
    Object.assign(form, {
      categoryId: d.categoryId,
      name: d.name,
      subtitle: d.subtitle || '',
      mainImage: d.mainImage || '',
      images: d.images || '',
      detail: d.detail || '',
      price: d.price ?? '',
      originalPrice: d.originalPrice ?? '',
      stock: d.stock ?? 0,
      status: d.status ?? 0,
      specs: d.specs || '',
      skus: d.skus || '',
    })
    specOpen.value = hasSpecs()
    syncSkuRows()
    drawer.value = true
  } catch (e) {
    toast.error(e.message || '加载商品详情失败')
  }
}

async function save() {
  if (!form.categoryId) return toast.error('请选择分类')
  if (!form.name.trim()) return toast.error('商品名称不能为空')
  if (!form.mainImage.trim()) return toast.error('主图不能为空')
  if (!form.price) return toast.error('售价不能为空')

  if (hasSpecs()) {
    // 前端预校验 JSON，格式错误直接拦截（后端仍会二次校验）
    try {
      JSON.parse(form.specs)
      JSON.parse(form.skus)
    } catch (e) {
      return toast.error('规格 JSON 格式错误，请检查')
    }
  }

  saving.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      subtitle: form.subtitle.trim() || null,
      mainImage: form.mainImage.trim(),
      images: form.images.trim() || null,
      detail: form.detail,
      price: Number(form.price),
      originalPrice: form.originalPrice ? Number(form.originalPrice) : null,
      stock: hasSpecs() ? 0 : Number(form.stock || 0),
      status: Number(form.status),
      specs: hasSpecs() ? form.specs.trim() : null,
      skus: hasSpecs() ? form.skus.trim() : null,
    }
    if (editingId.value) {
      await updateAdminProduct(editingId.value, payload)
      toast.success('已保存')
    } else {
      await createAdminProduct(payload)
      toast.success('新增成功')
    }
    drawer.value = false
    load()
  } catch (e) {
    toast.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function clearSpecs() {
  form.specs = ''
  form.skus = ''
  specOpen.value = false
  skuRows.value = []
}

async function doToggleStatus() {
  const target = statusTarget.value
  statusTarget.value = null
  const to = target.to
  try {
    await changeProductStatus(target.id, to)
    toast.success(to === 1 ? '已上架' : '已下架')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

async function doDelete() {
  const target = deleteTarget.value
  deleteTarget.value = null
  try {
    await deleteAdminProduct(target.id)
    toast.success('已删除')
    load()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

onMounted(() => {
  load()
  loadCategories().catch(() => {})
})
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">商品管理</h1>
      <button class="add-btn" @click="openCreate">＋ 新增商品</button>
    </div>

    <div class="filters">
      <input
        v-model="keyword"
        class="input"
        type="text"
        placeholder="商品名称"
        @keyup.enter="search"
      />
      <select v-model="categoryId" class="input select" @change="search">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <select v-model="statusFilter" class="input select" @change="search">
        <option value="">全部状态</option>
        <option value="1">上架</option>
        <option value="0">下架</option>
      </select>
      <button class="op search-btn" @click="search">搜索</button>
      <button
        v-if="keyword || categoryId || statusFilter !== ''"
        class="op"
        @click="keyword = ''; categoryId = ''; statusFilter = ''; search()"
      >
        重置
      </button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无商品</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>商品</th>
          <th>分类</th>
          <th>售价</th>
          <th>原价</th>
          <th>库存</th>
          <th>销量</th>
          <th>状态</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in records" :key="p.id">
          <td>
            <div class="prod">
              <img :src="p.mainImage" :alt="p.name" class="thumb" />
              <span class="prod-name">{{ p.name }}</span>
            </div>
          </td>
          <td>{{ p.categoryName || '-' }}</td>
          <td class="mono">¥{{ Number(p.price).toFixed(2) }}</td>
          <td class="mono">{{ p.originalPrice ? '¥' + Number(p.originalPrice).toFixed(2) : '-' }}</td>
          <td class="mono">{{ p.stock }}</td>
          <td class="mono">{{ p.sales }}</td>
          <td>
            <span class="status" :class="p.status === 1 ? 'on' : 'off'">
              {{ p.status === 1 ? '上架' : '下架' }}
            </span>
          </td>
          <td class="mono">{{ fmtTime(p.updateTime) }}</td>
          <td class="ops">
            <button class="op" @click="openEdit(p)">编辑</button>
            <button
              v-if="p.status === 1"
              class="op"
              @click="statusTarget = { id: p.id, name: p.name, to: 0 }"
            >
              下架
            </button>
            <button
              v-else
              class="op primary"
              @click="statusTarget = { id: p.id, name: p.name, to: 1 }"
            >
              上架
            </button>
            <button class="op danger" @click="deleteTarget = p">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="records.length" class="pager">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages() }}（共 {{ total }} 件）</span>
      <button class="page-btn" :disabled="page >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 新增/编辑抽屉 -->
    <div v-if="drawer" class="mask" @click.self="drawer = false">
      <aside class="drawer">
        <div class="drawer-head">
          <h2>{{ editingId ? '编辑商品' : '新增商品' }}</h2>
          <button class="close" @click="drawer = false">×</button>
        </div>

        <div class="form">
          <label class="field">
            <span>分类 <em>*</em></span>
            <select v-model.number="form.categoryId" class="input">
              <option :value="null" disabled>请选择分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <label class="field">
            <span>商品名称 <em>*</em></span>
            <input v-model.trim="form.name" class="input" type="text" maxlength="100" placeholder="商品名称" />
          </label>
          <label class="field">
            <span>副标题</span>
            <input v-model.trim="form.subtitle" class="input" type="text" maxlength="200" placeholder="卖点/副标题" />
          </label>
          <label class="field">
            <span>主图 URL <em>*</em></span>
            <input v-model.trim="form.mainImage" class="input" type="text" placeholder="https://…" />
          </label>
          <label class="field">
            <span>轮播图 URL（逗号分隔）</span>
            <input v-model.trim="form.images" class="input" type="text" placeholder="https://…,https://…" />
          </label>
          <label class="field">
            <span>商品详情（HTML）</span>
            <textarea v-model="form.detail" class="input textarea" rows="4" placeholder="富文本/HTML，可留空" />
          </label>

          <div class="row3">
            <label class="field">
              <span>售价 <em>*</em></span>
              <input v-model="form.price" class="input" type="number" min="0.01" step="0.01" />
            </label>
            <label class="field">
              <span>原价</span>
              <input v-model="form.originalPrice" class="input" type="number" min="0.01" step="0.01" />
            </label>
            <label class="field">
              <span>库存</span>
              <input
                v-model.number="form.stock"
                class="input"
                type="number"
                min="0"
                :disabled="hasSpecs()"
              />
            </label>
          </div>
          <p v-if="hasSpecs()" class="field-tip">
            有规格商品总库存 {{ skuTotalStock() }}，保存时由 SKU 库存自动汇总，此输入框禁用
          </p>

          <label class="field switch-row">
            <span>上架状态</span>
            <div class="switches">
              <button
                type="button"
                class="switch"
                :class="{ active: form.status === 1 }"
                @click="form.status = 1"
              >
                上架
              </button>
              <button
                type="button"
                class="switch"
                :class="{ active: form.status === 0 }"
                @click="form.status = 0"
              >
                下架
              </button>
            </div>
          </label>

          <section class="specs">
            <div class="specs-head">
              <h3>规格 / SKU（可选）</h3>
              <div class="specs-ops">
                <button v-if="!hasSpecs()" class="op" @click="specOpen = !specOpen">
                  {{ specOpen ? '收起配置' : '＋ 配置规格' }}
                </button>
                <button v-else class="op danger" @click="clearSpecs">设为无规格</button>
              </div>
            </div>
            <p v-if="!hasSpecs() && !specOpen" class="field-tip">无规格商品直接使用上方库存字段；配置规格后需同时填写 specs 与 skus JSON。</p>
            <template v-if="hasSpecs() || specOpen">
              <div v-if="hasSpecs()" class="sku-editor">
                <div class="sku-editor-head">
                  <span>SKU 库存 / 价格编辑（自动写回 skus JSON）</span>
                  <button type="button" class="op" @click="syncSkuRows">从 JSON 重新解析</button>
                </div>
                <table v-if="skuRows.length" class="sku-table">
                  <thead>
                    <tr>
                      <th>规格组合</th>
                      <th>售价</th>
                      <th>原价</th>
                      <th>库存</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, i) in skuRows" :key="row.id ?? i">
                      <td class="sku-spec">{{ skuRowLabel(row) }}</td>
                      <td>
                        <input v-model.number="row.price" class="input sku-num" type="number" min="0.01" step="0.01" @change="onSkuInput" />
                      </td>
                      <td>
                        <input v-model.number="row.originalPrice" class="input sku-num" type="number" min="0.01" step="0.01" @change="onSkuInput" />
                      </td>
                      <td>
                        <input v-model.number="row.stock" class="input sku-num" type="number" min="0" step="1" @change="onSkuInput" />
                      </td>
                      <td class="sku-del">
                        <button type="button" class="op danger" @click="removeSkuRow(i)">删除</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <p v-else class="field-tip sku-empty">暂无 SKU 数据，请在下方 skus JSON 中填写，或点击「＋ 新增 SKU」。</p>
                <div class="sku-editor-foot">
                  <span class="sku-total">总库存：<b>{{ skuTotalStock() }}</b></span>
                  <button type="button" class="op" @click="addSkuRow">＋ 新增 SKU</button>
                </div>
              </div>
              <label class="field">
                <span>specs（规格维度）</span>
                <textarea v-model="form.specs" class="input textarea textarea-mono" rows="4"
                  placeholder='[{"name":"颜色","values":["黑色","白色"]}]' />
              </label>
              <label class="field">
                <span>skus（SKU 列表）</span>
                <textarea v-model="form.skus" class="input textarea textarea-mono" rows="6"
                  placeholder='[{"id":101,"specs":{"颜色":"黑色"},"price":399,"stock":10}]' />
              </label>
            </template>
          </section>
        </div>

        <div class="drawer-foot">
          <button class="foot-btn cancel" @click="drawer = false">取消</button>
          <button class="foot-btn primary" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </aside>
    </div>

    <!-- 上下架确认 -->
    <div v-if="statusTarget" class="mask" @click.self="statusTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">{{ statusTarget.to === 1 ? '确认上架' : '确认下架' }}</h3>
        <p class="confirm-desc">
          {{ statusTarget.to === 1 ? '上架后买家可在前台购买「' + statusTarget.name + '」' : '下架后买家不可再购买「' + statusTarget.name + '」，已产生的订单不受影响' }}
          ，确定吗？
        </p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="statusTarget = null">再想想</button>
          <button class="confirm-btn primary" @click="doToggleStatus">确定</button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="deleteTarget" class="mask" @click.self="deleteTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">确认删除</h3>
        <p class="confirm-desc">删除「{{ deleteTarget.name }}」？存在交易/收藏引用的商品将被拒绝并提示改为下架。</p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="deleteTarget = null">再想想</button>
          <button class="confirm-btn danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 共享样式见 assets/admin.css（.admin-page 包装类），此处仅保留页面独有样式 */
.prod {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 12.5rem;
}
.thumb {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 0.5rem;
  object-fit: cover;
  background: var(--bg-gray);
  flex-shrink: 0;
}
.prod-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 13.75rem;
}
.textarea-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.75rem;
}
.row3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}
.field-tip {
  margin: -0.25rem 0 0;
  font-size: 0.75rem;
  color: var(--ink-faint);
}
.textarea {
  height: auto;
  padding: 0.625rem 0.75rem;
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
}
.switches {
  display: flex;
  gap: 0.5rem;
}
.switch {
  height: 2rem;
  padding: 0 1.125rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  background: var(--bg);
  color: var(--ink-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
}
.switch.active {
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.08);
  color: var(--blue);
  font-weight: 600;
}
.specs {
  border: 1px dashed var(--border);
  border-radius: var(--card-radius);
  padding: 0.875rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.specs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.specs-head h3 {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ink);
}
.sku-editor {
  border: 1px solid var(--border);
  border-radius: var(--card-radius);
  overflow: hidden;
}
.sku-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  background: var(--bg-gray);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--ink);
}
.sku-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}
.sku-table th,
.sku-table td {
  padding: 0.5rem 0.75rem;
  border-top: 1px solid var(--border);
  text-align: left;
  vertical-align: middle;
}
.sku-table th {
  color: var(--ink-secondary);
  font-weight: 500;
  background: var(--bg);
}
.sku-spec {
  min-width: 11rem;
}
.sku-num {
  width: 5.5rem;
}
.sku-empty {
  padding: 0.625rem 0.75rem;
  margin: 0;
}
.sku-editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  border-top: 1px solid var(--border);
}
.sku-total {
  font-size: 0.8125rem;
  color: var(--ink-secondary);
}
.sku-total b {
  color: var(--ink);
}
.specs-ops {
  display: flex;
  gap: 0.5rem;
}
.drawer-foot {
  display: flex;
  gap: 0.625rem;
  margin-top: 1.5rem;
  position: sticky;
  bottom: -1.5rem;
  background: var(--bg);
  padding: 0.75rem 0 0;
}
.foot-btn {
  flex: 1;
  height: 2.625rem;
  border: none;
  border-radius: 0.625rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
.foot-btn.cancel {
  background: var(--bg-gray);
  color: var(--ink);
}
.foot-btn.primary {
  background: var(--blue);
  color: #fff;
}
.foot-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
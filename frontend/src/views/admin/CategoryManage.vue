<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  changeCategoryStatus,
  createAdminCategory,
  deleteAdminCategory,
  fetchAdminCategories,
  updateAdminCategory,
} from '@/api/admin/category'
import { toast } from '@/utils/toast'

const records = ref([])
const loading = ref(false)

const modal = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', sort: 1 })

const statusTarget = ref(null)
const deleteTarget = ref(null)

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    records.value = await fetchAdminCategories()
  } catch (e) {
    toast.error(e.message || '分类加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sort = 1
  modal.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  modal.value = true
}

async function save() {
  if (!form.name.trim()) return toast.error('分类名称不能为空')
  saving.value = true
  try {
    const payload = { name: form.name.trim(), sort: Number(form.sort || 0) }
    if (editingId.value) {
      await updateAdminCategory(editingId.value, payload)
      toast.success('已保存')
    } else {
      await createAdminCategory(payload)
      toast.success('新增成功')
    }
    modal.value = false
    load()
  } catch (e) {
    toast.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function doToggleStatus() {
  const target = statusTarget.value
  statusTarget.value = null
  try {
    await changeCategoryStatus(target.id, target.to)
    toast.success(target.to === 1 ? '已启用' : '已停用')
    load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

async function doDelete() {
  const target = deleteTarget.value
  deleteTarget.value = null
  try {
    await deleteAdminCategory(target.id)
    toast.success('已删除')
    load()
  } catch (e) {
    toast.error(e.message || '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <h1 class="title">分类管理</h1>
      <button class="add-btn" @click="openCreate">＋ 新增分类</button>
    </div>

    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="records.length === 0" class="hint">暂无分类</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>分类名</th>
          <th>排序</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in records" :key="c.id">
          <td>{{ c.name }}</td>
          <td class="mono">{{ c.sort }}</td>
          <td>
            <span class="status" :class="c.status === 1 ? 'on' : 'off'">
              {{ c.status === 1 ? '启用' : '已停用' }}
            </span>
          </td>
          <td class="mono">{{ fmtTime(c.createTime) }}</td>
          <td class="ops">
            <button class="op" @click="openEdit(c)">编辑</button>
            <button
              v-if="c.status === 1"
              class="op"
              @click="statusTarget = { id: c.id, name: c.name, to: 0 }"
            >
              停用
            </button>
            <button v-else class="op primary" @click="statusTarget = { id: c.id, name: c.name, to: 1 }">
              启用
            </button>
            <button class="op danger" @click="deleteTarget = c">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 新增/编辑弹窗 -->
    <div v-if="modal" class="mask" @click.self="modal = false">
      <div class="modal">
        <h3 class="modal-title">{{ editingId ? '编辑分类' : '新增分类' }}</h3>
        <label class="field">
          <span>分类名称</span>
          <input v-model.trim="form.name" class="input" type="text" maxlength="50" placeholder="如：家具家电" />
        </label>
        <label class="field">
          <span>排序值（越小越靠前）</span>
          <input v-model.number="form.sort" class="input" type="number" />
        </label>
        <div class="modal-ops">
          <button class="op cancel" @click="modal = false">取消</button>
          <button class="op primary" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 启停确认 -->
    <div v-if="statusTarget" class="mask" @click.self="statusTarget = null">
      <div class="confirm">
        <h3 class="confirm-title">{{ statusTarget.to === 1 ? '确认启用' : '确认停用' }}</h3>
        <p class="confirm-desc">停用后「{{ statusTarget.name }}」分类将对买家隐藏，确定吗？</p>
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
        <p class="confirm-desc">删除「{{ deleteTarget.name }}」？分类下存在商品时会被拒绝。</p>
        <div class="confirm-ops">
          <button class="confirm-btn cancel" @click="deleteTarget = null">再想想</button>
          <button class="confirm-btn danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<!-- 共享样式见 assets/admin.css（.admin-page 包装类），本页无独有样式 -->

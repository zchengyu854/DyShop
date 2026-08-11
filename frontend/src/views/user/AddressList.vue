<script setup>
import { computed, onMounted, shallowRef } from 'vue'
import AddressFormModal from '@/components/user/AddressFormModal.vue'
import { deleteAddress, fetchAddresses, setDefaultAddress } from '@/api/address'
import { toast } from '@/utils/toast'

const MAX_COUNT = 20

const addresses = shallowRef([])
const loading = shallowRef(true)
const modalOpen = shallowRef(false)
const editing = shallowRef(null)
const confirming = shallowRef(null)
const defaultBusy = shallowRef(false)

const count = computed(() => addresses.value.length)
const atLimit = computed(() => count.value >= MAX_COUNT)
// 虚线占位卡是唯一新增入口：未达上限时始终显示（无需数量限制）
const showAddCard = computed(() => !atLimit.value)

function maskPhone(phone) {
  return phone && phone.length >= 11 ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone
}

async function load() {
  loading.value = true
  try {
    addresses.value = await fetchAddresses()
  } catch (e) {
    toast.error(e.message || '地址加载失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editing.value = null
  modalOpen.value = true
}

function openEdit(addr) {
  editing.value = addr
  modalOpen.value = true
}

function handleSaved() {
  load()
}

// 设为默认：乐观更新（本地置首 + isDefault 互斥）配合 FLIP 动画，成功后以后端为准
async function handleSetDefault(addr) {
  if (defaultBusy.value) return
  defaultBusy.value = true
  const prev = addresses.value
  const next = [...addresses.value].filter((a) => a.id !== addr.id)
  addresses.value = [{ ...addr, isDefault: 1 }, ...next.map((a) => ({ ...a, isDefault: 0 }))]
  try {
    await setDefaultAddress(addr.id)
    toast.success('已设为默认地址')
    load()
  } catch (e) {
    addresses.value = prev
    toast.error(e.message || '操作失败，请重试')
  } finally {
    defaultBusy.value = false
  }
}

function handleDelete(addr) {
  confirming.value = addr
}

async function confirmDelete() {
  const addr = confirming.value
  try {
    await deleteAddress(addr.id)
    toast.success('地址已删除')
    confirming.value = null
    load()
  } catch (e) {
    toast.error(e.message || '删除失败，请重试')
    confirming.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="addr-panel">
    <div class="head">
      <div class="head-title">
        <h1 class="title">收货地址</h1>
        <span class="count" :class="{ 'count-full': atLimit }">{{ count }}/{{ MAX_COUNT }}</span>
      </div>
    </div>

      <div v-if="loading" class="hint">加载中…</div>

      <div v-else-if="count === 0" class="empty">
        <svg class="empty-icon" viewBox="0 0 64 64" fill="none" aria-hidden="true">
          <circle cx="32" cy="32" r="28" stroke="#d2d2d7" stroke-width="2" />
          <path
            d="M18 30c6-1 10-7 14-7s8 5 14 7v14a2 2 0 0 1-2 2H20a2 2 0 0 1-2-2V30Z"
            fill="#e8e8ed"
          />
          <path d="M18 30l14 9 14-9" stroke="#86868b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <circle cx="32" cy="20" r="4" stroke="#0071e3" stroke-width="2" />
        </svg>
        <p class="empty-title">还没有收货地址</p>
        <p class="empty-sub">保存常用地址，下单时免重复填写</p>
        <button class="empty-btn" @click="openAdd">新增地址</button>
      </div>

      <TransitionGroup v-else name="flip" tag="div" class="grid">
        <article v-for="addr in addresses" :key="addr.id" class="card">
          <header class="card-head">
            <h2 class="card-name">{{ addr.receiverName }}</h2>
            <span class="card-phone">{{ maskPhone(addr.receiverPhone) }}</span>
            <span v-if="addr.isDefault" class="default-tag">默认</span>
          </header>

          <p class="card-address" :title="addr.fullAddress">{{ addr.fullAddress }}</p>

          <footer class="card-actions">
            <button
              v-if="!addr.isDefault"
              class="action-btn"
              :disabled="defaultBusy"
              @click="handleSetDefault(addr)"
            >
              设为默认
            </button>
            <button class="action-btn" @click="openEdit(addr)">编辑</button>
            <button class="action-btn danger" @click="handleDelete(addr)">删除</button>
          </footer>
        </article>

        <button v-if="showAddCard" key="add-card" class="add-card" @click="openAdd">
          <span class="add-card-plus">＋</span>
          <span>新增地址</span>
        </button>
      </TransitionGroup>

    <AddressFormModal v-model="modalOpen" :address="editing" @saved="handleSaved" />

    <Teleport to="body">
      <div v-if="confirming" class="confirm-overlay" @click.self="confirming = null">
        <div class="confirm" role="alertdialog" aria-modal="true" aria-label="删除确认">
          <h3 class="confirm-title">删除收货地址</h3>
          <p class="confirm-text">
            确定删除「{{ confirming.receiverName }}」的收货地址吗？
          </p>
          <p v-if="confirming.isDefault" class="confirm-hint">
            该地址为默认地址，删除后将自动把最近添加的地址设为默认。
          </p>
          <div class="confirm-actions">
            <button class="confirm-cancel" @click="confirming = null">取消</button>
            <button class="confirm-ok" @click="confirmDelete">删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.addr-panel {
  width: 100%;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.head-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--ink);
}
.count {
  font-size: 13px;
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}
.count-full {
  color: #ff9500;
  font-weight: 600;
}
.hint {
  padding: 60px 0;
  text-align: center;
  color: var(--ink-secondary);
  font-size: 14px;
}

/* ---- 空态 ---- */
.empty {
  padding: 72px 0 88px;
  text-align: center;
}
.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
}
.empty-title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
  color: var(--ink);
}
.empty-sub {
  margin: 10px 0 24px;
  color: var(--ink-secondary);
  font-size: 14px;
}
.empty-btn {
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
.empty-btn:hover {
  background: #0077ed;
}

/* ---- 网格（个人中心内容区约 792px：2 列为主，窄屏自然收为 1 列） ---- */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

/* ---- 卡片 ---- */
.card {
  display: flex;
  flex-direction: column;
  min-height: 176px;
  padding: 20px 24px;
  background: var(--bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-phone {
  font-size: 14px;
  color: var(--ink-secondary);
  font-variant-numeric: tabular-nums;
}
.default-tag {
  margin-left: auto;
  flex: none;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: rgba(0, 113, 227, 0.12);
  color: #0063c1;
  font-size: 11px;
  font-weight: 600;
}
.card-address {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
@media (min-width: 641px) {
  .card:hover .card-address {
    -webkit-line-clamp: unset;
    overflow: visible;
    background: var(--bg);
    z-index: 1;
  }
}

/* ---- 操作区：右对齐停靠 + 36px 热区 ---- */
.card-actions {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--border-line);
}
.action-btn {
  height: 36px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.action-btn:hover {
  background: var(--bg-gray);
  color: var(--ink);
}
.action-btn.danger:hover {
  background: rgba(255, 59, 48, 0.08);
  color: #ff3b30;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: default;
}
@media (max-width: 640px) {
  .action-btn {
    height: 44px;
    padding: 0 14px;
  }
}

/* ---- 虚线占位卡 ---- */
.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 176px;
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-card);
  background: transparent;
  color: var(--ink-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}
.add-card:hover {
  color: var(--blue);
  border-color: var(--blue);
  background: rgba(0, 113, 227, 0.05);
}
.add-card-plus {
  font-size: 26px;
  line-height: 1;
  transition: transform 0.2s ease;
}
.add-card:hover .add-card-plus {
  transform: scale(1.25);
}
.add-card:focus-visible {
  outline: 2px solid var(--blue);
  outline-offset: 2px;
  border-color: var(--blue);
}

/* ---- FLIP 移动动画 ---- */
.flip-move {
  transition: transform 0.3s ease;
}

/* ---- 删除确认弹窗 ---- */
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 110;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.4);
}
.confirm {
  width: 100%;
  max-width: 360px;
  background: var(--bg);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}
.confirm-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
}
.confirm-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink);
}
.confirm-hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--ink-secondary);
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--bg-gray);
}
.confirm-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}
.confirm-cancel,
.confirm-ok {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-full);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}
.confirm-cancel {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink);
}
.confirm-cancel:hover {
  background: var(--bg-gray);
}
.confirm-ok {
  border: none;
  background: #ff3b30;
  color: #fff;
  font-weight: 600;
}
.confirm-ok:hover {
  background: #ff5447;
}
</style>
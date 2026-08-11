import { reactive } from 'vue'

// 全局无图标 Toast：模块级队列，供任意组件调用。
// 自动关闭时长：成功/信息 4s，警告 6s，错误 8s；最多同屏 4 条，超出丢弃最早条目。
const DURATION = { success: 4000, error: 8000, warning: 6000, info: 4000 }
const MAX_TOASTS = 4
const LEAVE_MS = 180

let seed = 0

export const toasts = reactive([])

function push(type, message, title) {
  const id = ++seed
  const item = reactive({ id, type, message, title, leaving: false, timer: null })
  if (toasts.length >= MAX_TOASTS) {
    const first = toasts.shift()
    clearTimeout(first.timer)
  }
  toasts.push(item)
  item.timer = setTimeout(() => dismiss(id), DURATION[type])
  return id
}

export function dismiss(id) {
  const item = toasts.find((t) => t.id === id)
  if (!item || item.leaving) return
  clearTimeout(item.timer)
  item.leaving = true
  setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.splice(idx, 1)
  }, LEAVE_MS)
}

// 悬停暂停/恢复自动关闭
export function pause(id) {
  const item = toasts.find((t) => t.id === id)
  if (item && !item.leaving) clearTimeout(item.timer)
}

export function resume(id) {
  const item = toasts.find((t) => t.id === id)
  if (item && !item.leaving) item.timer = setTimeout(() => dismiss(id), DURATION[item.type])
}

export const toast = {
  success: (message, title) => push('success', message, title),
  error: (message, title) => push('error', message, title),
  warning: (message, title) => push('warning', message, title),
  info: (message, title) => push('info', message, title),
}

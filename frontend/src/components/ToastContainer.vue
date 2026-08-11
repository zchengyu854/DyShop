<script setup>
import { dismiss, pause, resume, toasts } from '@/utils/toast'

const TAG_TEXT = {
  success: '成功',
  error: '错误',
  warning: '警告',
  info: '提示',
}

function tagText(type) {
  return TAG_TEXT[type] || '提示'
}
</script>

<template>
  <Teleport to="body">
    <div class="toast-stack">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="[`toast--${t.type}`, { 'toast--leaving': t.leaving }]"
        :role="t.type === 'error' ? 'alert' : 'status'"
        :aria-live="t.type === 'error' ? 'assertive' : 'polite'"
        aria-atomic="true"
        @mouseenter="pause(t.id)"
        @mouseleave="resume(t.id)"
      >
        <span class="toast__bar" aria-hidden="true" />
        <div class="toast__content">
          <span class="toast__tag">{{ tagText(t.type) }}</span>
          <p v-if="t.title" class="toast__title">{{ t.title }}</p>
          <p class="toast__body">{{ t.message }}</p>
        </div>
        <button class="toast__close" aria-label="关闭通知" @click="dismiss(t.id)">×</button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
}
.toast {
  pointer-events: auto;
  display: flex;
  align-items: stretch;
  min-width: 320px;
  max-width: 400px;
  padding: 12px 14px 12px 18px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  animation: toast-in 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.toast__bar {
  flex: none;
  width: 4px;
  border-radius: 2px;
  align-self: stretch;
  margin-right: 8px;
}
.toast__content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.toast__tag {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.toast__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.toast__body {
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.55;
  color: #374151;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.toast__close {
  flex: none;
  width: 28px;
  height: 28px;
  margin: -4px -6px -4px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 120ms, background 120ms;
}
.toast:hover .toast__close {
  opacity: 1;
}
.toast__close:hover {
  background: rgba(17, 24, 39, 0.06);
  color: #111827;
}

.toast--success .toast__bar { background: #10b981; }
.toast--success .toast__tag { color: #047857; }
.toast--error .toast__bar { background: #ef4444; }
.toast--error .toast__tag { color: #b91c1c; }
.toast--warning .toast__bar { background: #f59e0b; }
.toast--warning .toast__tag { color: #92400e; }
.toast--info .toast__bar { background: #3b82f6; }
.toast--info .toast__tag { color: #1d4ed8; }

.toast--leaving {
  animation: toast-out 180ms ease-in forwards;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-4px);
  }
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  50% { transform: translateX(4px); }
  75% { transform: translateX(-2px); }
}
.toast--error {
  animation:
    toast-in 200ms cubic-bezier(0.16, 1, 0.3, 1),
    shake 240ms 60ms;
}

@media (prefers-reduced-motion: reduce) {
  .toast,
  .toast--error {
    animation: toast-in 120ms linear;
  }
  .toast--error {
    animation-name: toast-in;
  }
  .toast--leaving {
    animation: toast-out 120ms linear forwards;
  }
}

@media (prefers-color-scheme: dark) {
  .toast {
    background: #18181b;
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }
  .toast__title {
    color: #fafafa;
  }
  .toast__body {
    color: #d1d5db;
  }
  .toast__close {
    color: #9ca3af;
  }
  .toast__close:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fafafa;
  }
  .toast--success .toast__bar { background: #34d399; }
  .toast--success .toast__tag { color: #34d399; }
  .toast--error .toast__bar { background: #f87171; }
  .toast--error .toast__tag { color: #f87171; }
  .toast--warning .toast__bar { background: #fbbf24; }
  .toast--warning .toast__tag { color: #fbbf24; }
  .toast--info .toast__bar { background: #60a5fa; }
  .toast--info .toast__tag { color: #60a5fa; }
}
</style>

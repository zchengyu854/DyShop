<script setup>
const year = new Date().getFullYear()
</script>

<template>
  <footer class="footer">
    © {{ year }} dyshop 电子好物商店 · 保留所有权利
  </footer>
</template>

<style scoped>
.footer {
  text-align: center;
  padding: 26px 20px;
  font-size: 12px;
  color: var(--ink-faint);
  background: var(--bg-gray);
  border-top: 1px solid var(--border);
}
</style>

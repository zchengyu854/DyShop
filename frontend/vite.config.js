import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发代理：/api → dyshop-api 服务（8081）
// 说明：后台 controller 全部位于 dyshop-api 模块（路径 /api/admin/**），
//       dyshop-admin 模块为空壳（仅启动类），后台请求与 C 端同走 8081。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})

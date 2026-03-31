import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devProxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8142'

  return {
    plugins: [
      vue(),
      vueJsx(),
      vueDevTools(),
    ],
    server: {
      port: 5876,
      proxy: {
        '/api': {
          target: devProxyTarget,
          changeOrigin: true,
        },
      },
      /**
       * Browser → http://localhost:5876  (Vite frontend on WSL)
       *                  ↓ (跨域请求，不同 host)
       *                  ↓ (Cookie 不会被发送 or 保存)
       *                  ↓ (所以需要 proxy 让浏览器以为同域)
       * API  → http://172.25.160.1:8142  (backend on Windows)
       */
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
  }
})

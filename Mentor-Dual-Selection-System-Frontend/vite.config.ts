import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = (env.VITE_API_PROXY_TARGET || 'http://223.82.75.76:50000').replace(/\/$/, '')
  const devHost = env.VITE_DEV_HOST || '0.0.0.0'
  const devPort = Number(env.VITE_DEV_PORT || '5173')

  return {
    plugins: [
      vue(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: devHost,
      port: devPort,
      proxy: {
        '/api': {
          target: `${apiBase}/api`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
        '/admin': {
          target: `${apiBase}/admin`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/admin/, ''),
        },
        '/teacher': {
          target: `${apiBase}/teacher`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/teacher/, ''),
        },
        '/student': {
          target: `${apiBase}/student`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/student/, ''),
        },
        '/upload': {
          target: `${apiBase}/upload`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/upload/, ''),
        },
      },
    }
  }
})

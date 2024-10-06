import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://223.82.75.76:50000/api',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/admin': {
        target: 'http://223.82.75.76:50000/admin',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/admin/, ''),
      },
      '/teacher': {
        target: 'http://223.82.75.76:50000/teacher',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/teacher/, ''),
      },
      '/student': {
        target: 'http://223.82.75.76:50000/student',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/student/, ''),
      },
      '/upload': {
        target: 'http://223.82.75.76:50000/upload',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/upload/, ''),
      },
    },
  },
})

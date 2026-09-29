import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  // Permite node_modules como link simbólico (ex.: node_modules -> node_modules.nosync, fora do iCloud).
  resolve: { preserveSymlinks: true },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

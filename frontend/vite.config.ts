import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  // Permite node_modules como link simbólico (ex.: node_modules -> node_modules.nosync, fora do iCloud).
  resolve: { preserveSymlinks: true },
  server: {
    proxy: {
      '/api': {
        // Os testes de ponta a ponta apontam para um backend próprio (ver playwright.config.ts).
        target: process.env.VITE_API_PROXY ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  // Os testes de ponta a ponta usam um cache próprio: dividir o cache com o servidor de desenvolvimento
  // faz um invalidar o do outro, e o Vite recarrega a página no meio do teste.
  cacheDir: process.env.VITE_CACHE_DIR ?? 'node_modules/.vite',
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

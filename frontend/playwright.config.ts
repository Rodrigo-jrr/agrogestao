import { defineConfig, devices } from '@playwright/test'

/**
 * Testes de ponta a ponta: sobem um backend (8081) e um frontend (5174) só para eles, sobre o MySQL e o
 * Mailpit do docker-compose. Rodar: `docker compose up -d` na raiz e `npm run test:e2e` aqui.
 * Localmente usa o Google Chrome instalado; no CI, o Chromium do Playwright.
 */
const BACKEND_PORT = 8081
const FRONTEND_PORT = 5174
const CI = !!process.env.CI

export default defineConfig({
  testDir: './e2e',
  // Um de cada vez: os testes compartilham o banco e a caixa de e-mail.
  workers: 1,
  retries: CI ? 1 : 0,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://localhost:${FRONTEND_PORT}`,
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], channel: CI ? undefined : 'chrome' },
      testIgnore: /\.mobile\.spec\.ts$/,
    },
    {
      name: 'celular',
      use: { ...devices['Pixel 7'], channel: CI ? undefined : 'chrome' },
      testMatch: /\.mobile\.spec\.ts$/,
    },
  ],
  webServer: [
    {
      command: 'mvn -q spring-boot:run',
      cwd: '../backend',
      url: `http://localhost:${BACKEND_PORT}/api/me`, // 401 já indica que subiu
      reuseExistingServer: !CI,
      timeout: 240_000,
      env: {
        SERVER_PORT: String(BACKEND_PORT),
        APP_BASE_URL: `http://localhost:${FRONTEND_PORT}`,
        // Cada teste cria contas novas a partir do mesmo IP.
        REGISTER_MAX_PER_IP_PER_HOUR: '100000',
        LOGIN_MAX_FAILURES_PER_IP: '100000',
        INVITATIONS_MAX_PER_PROPERTY_PER_DAY: '100000',
      },
    },
    {
      command: `npm run dev -- --port ${FRONTEND_PORT} --strictPort`,
      url: `http://localhost:${FRONTEND_PORT}`,
      reuseExistingServer: !CI,
      timeout: 60_000,
      env: { VITE_API_PROXY: `http://localhost:${BACKEND_PORT}`, VITE_CACHE_DIR: 'node_modules/.vite-e2e' },
    },
  ],
})

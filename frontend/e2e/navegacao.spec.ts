import { expect, test } from '@playwright/test'

test('páginas internas exigem login', async ({ page }) => {
  await page.goto('/financeiro')
  await expect(page).toHaveURL(/\/login$/)
})

test('endereço inexistente mostra a página de não encontrado', async ({ page }) => {
  await page.goto('/pagina-que-nao-existe')
  await expect(page.getByRole('heading', { name: 'Página não encontrada' })).toBeVisible()
})

test('termos e política de privacidade são públicos', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('link', { name: 'Política de privacidade' }).click()
  await expect(page.getByRole('heading', { name: 'Política de privacidade', level: 1 })).toBeVisible()
  await page.getByRole('navigation', { name: 'Documentos' }).getByRole('link', { name: 'Termos de uso' }).click()
  await expect(page.getByRole('heading', { name: 'Termos de uso', level: 1 })).toBeVisible()
})

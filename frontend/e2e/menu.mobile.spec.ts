import { expect, test } from '@playwright/test'
import { createOwner, signIn } from './support'

test('no celular o menu abre como gaveta e fecha ao navegar, sem rolagem lateral', async ({ page, request }) => {
  const owner = await createOwner(request, 'Celular')
  await signIn(page, owner)

  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  // O texto do botão alterna entre "Menu" e "Fechar"; o que não muda é o que ele controla.
  const menu = page.locator('button[aria-controls="app-sidebar"]')
  await expect(menu).toHaveText('Menu')
  await expect(page.getByRole('complementary', { name: 'Menu principal' })).toBeHidden()
  await menu.click()
  await expect(menu).toHaveAttribute('aria-expanded', 'true')
  await expect(menu).toHaveText('Fechar')
  await page.getByRole('complementary', { name: 'Menu principal' }).getByRole('link', { name: 'Estoque' }).click()

  await expect(page).toHaveURL(/\/estoque$/)
  await expect(page.getByRole('complementary', { name: 'Menu principal' })).toBeHidden()
  for (const path of ['/estoque', '/financeiro', '/equipe', '/perfil']) {
    await page.goto(path)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow, `rolagem lateral em ${path}`).toBeLessThanOrEqual(0)
  }
})

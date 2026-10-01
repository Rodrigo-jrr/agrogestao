import { expect, test } from '@playwright/test'
import { PASSWORD, uniqueEmail } from './support'

test('cadastro com aceite dos termos, criação da propriedade e novo login', async ({ page }) => {
  const email = uniqueEmail('cadastro')

  await page.goto('/register')
  await page.waitForLoadState('networkidle')
  await page.getByLabel('Nome').fill('Joana Cadastro')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill(PASSWORD)

  // Sem aceitar os termos, o formulário não é enviado.
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page).toHaveURL(/\/register$/)

  await page.getByRole('checkbox', { name: /Termos de uso/ }).check()
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page).toHaveURL(/\/onboarding$/)
  await page.getByLabel('Nome da conta/organização').fill('Família Cadastro')
  await page.getByLabel('Nome da propriedade').fill('Fazenda Boa Vista')
  await page.getByRole('button', { name: '+ Horta' }).click()
  await page.getByRole('button', { name: 'Continuar' }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('banner').getByRole('combobox')).toHaveValue(/\d+/)
  await expect(page.getByRole('banner').getByRole('combobox').locator('option:checked')).toHaveText('Fazenda Boa Vista')

  await page.getByRole('button', { name: 'Sair' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill(PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
})

test('senha errada mostra o motivo sem entrar', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('E-mail').fill(uniqueEmail('ninguem'))
  await page.getByLabel('Senha').fill('qualquerCoisa1')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByText('Credenciais inválidas')).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
})

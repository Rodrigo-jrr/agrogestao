import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { PASSWORD, api, createOwner, signIn } from './support'

test('baixar meus dados gera o arquivo JSON com a conta e a propriedade', async ({ page, request }) => {
  const owner = await createOwner(request, 'Exportadora')
  await api(request, owner, 'post', '/activities', { name: 'Horta orgânica', type: 'HORTA' })
  await signIn(page, owner, '/perfil')

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Baixar meus dados' }).click(),
  ])
  expect(download.suggestedFilename()).toMatch(/^agrogestao-meus-dados-\d{4}-\d{2}-\d{2}\.json$/)
  const data = JSON.parse(await readFile(await download.path(), 'utf8'))
  expect(data.conta.email).toBe(owner.email)
  expect(data.aceitesDosTermos).toHaveLength(1)
  expect(JSON.stringify(data.propriedades[0].dados.atividades)).toContain('Horta orgânica')
})

test('excluir a conta apaga o acesso e avisa no login', async ({ page, request }) => {
  const owner = await createOwner(request, 'Despedida')
  await signIn(page, owner, '/perfil')

  // A propriedade aparece na lista do que será apagado junto.
  await expect(page.getByRole('main').getByRole('listitem').filter({ hasText: owner.propertyName })).toBeVisible()
  await page.getByRole('button', { name: 'Excluir minha conta…' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Sua senha').fill('senhaErrada1')
  await dialog.getByRole('button', { name: 'Excluir minha conta' }).click()
  await expect(dialog.getByRole('alert')).toHaveText('Senha incorreta.')

  await dialog.getByLabel('Sua senha').fill(PASSWORD)
  await dialog.getByRole('button', { name: 'Excluir minha conta' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('status')).toContainText('Sua conta foi excluída')

  await page.getByLabel('E-mail').fill(owner.email)
  await page.getByLabel('Senha').fill(PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByText('Credenciais inválidas')).toBeVisible()
})

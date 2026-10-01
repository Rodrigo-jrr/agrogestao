import { expect, test } from '@playwright/test'
import { createOwner, signIn } from './support'

test('agricultura: cadastra um talhão, vê na lista e arquiva', async ({ page, request }) => {
  const owner = await createOwner(request, 'Agricultor')

  await signIn(page, owner, '/agricultura')
  await page.getByLabel('Nome do talhão').fill('Talhão 1')
  await page.getByLabel('Área (ha)').fill('25.5')
  await page.getByLabel(/Localização/).fill('Fundo da propriedade')
  await page.getByLabel(/Tipo de solo/).fill('Argiloso')
  await page.getByRole('button', { name: 'Criar talhão' }).click()

  const row = page.getByRole('main').getByRole('listitem').filter({ hasText: 'Talhão 1' })
  await expect(row).toContainText('25,5')
  await expect(row).toContainText('Fundo da propriedade · Argiloso')

  // Marca "mostrar arquivados" antes: senão o talhão some da lista assim que for arquivado.
  await page.getByLabel('Mostrar arquivados').check()
  await row.getByRole('button', { name: 'Arquivar' }).click()
  await expect(row).toContainText('arquivado')

  // Desmarcando o filtro, some da lista (só havia esse talhão); volta ao marcar de novo.
  await page.getByLabel('Mostrar arquivados').uncheck()
  await expect(page.getByText('Nenhum talhão cadastrado ainda.')).toBeVisible()
  await page.getByLabel('Mostrar arquivados').check()
  await expect(row).toBeVisible()
})

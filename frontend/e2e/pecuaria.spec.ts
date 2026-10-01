import { expect, test } from '@playwright/test'
import { api, createOwner, signIn } from './support'

test('pecuária: lote, compra de animais com brinco, cria, venda e cancelamento', async ({ page, request }) => {
  const owner = await createOwner(request, 'Pecuarista')
  await api(request, owner, 'post', '/purchases/suppliers', { name: 'Leilão do Vale' })
  await api(request, owner, 'post', '/sales/customers', { name: 'Frigorífico Norte' })
  await api(request, owner, 'post', '/livestock/species', { name: 'Bovino', allowsIndividual: true })

  // Lote
  await signIn(page, owner, '/pecuaria?aba=lotes')
  await page.getByLabel('Nome do lote').fill('Vacas em lactação')
  await page.getByRole('button', { name: 'Criar lote' }).click()
  await expect(page.getByRole('main').getByRole('listitem').filter({ hasText: 'Vacas em lactação' })).toContainText('0 cabeças')

  // Compra de 3 vacas, colando os brincos
  await page.getByRole('tab', { name: 'Compras e vendas' }).click()
  await page.getByLabel('Fornecedor').selectOption({ label: 'Leilão do Vale' })
  await page.getByLabel('Sexo').selectOption('FEMEA')
  await page.getByLabel(/Brincos, um por linha/).fill('301\n302; Estrela\n303')
  await page.getByLabel(/^Lote/).selectOption({ label: 'Vacas em lactação' })
  await page.getByLabel('Valor total (R$)').fill('9000')
  await expect(page.getByText('R$ 3.000,00 por cabeça')).toBeVisible()
  await page.getByRole('button', { name: 'Registrar compra' }).click()
  await expect(page.getByRole('main').getByRole('status')).toHaveText('Compra de 3 cabeças registrada.')
  await expect(page.getByText('3 cabeças de Bovino')).toBeVisible()

  // Cria da 302, nascida hoje
  await page.getByRole('tab', { name: 'Rebanho' }).click()
  await page.getByRole('button', { name: 'Cadastrar animal' }).click()
  await page.getByLabel('Brinco', { exact: true }).fill('401')
  // O rádio fica visualmente escondido; quem usa o app clica no rótulo.
  await page.locator('label').filter({ hasText: /^Macho$/ }).click()
  await expect(page.getByRole('radio', { name: 'Macho' })).toBeChecked()
  await page.getByLabel(/^Mãe/).selectOption({ label: '302 — Estrela' })
  await page.getByLabel(/^Peso/).fill('32')
  await page.getByRole('button', { name: 'Cadastrar animal' }).click()
  await expect(page.getByRole('main').getByRole('status')).toHaveText('Animal 401 cadastrado.')

  // Ficha da mãe: linha do tempo e cria
  await page.getByRole('link', { name: '302' }).click()
  await expect(page.getByRole('heading', { name: /302/ })).toBeVisible()
  await expect(page.getByText('Compra de Leilão do Vale · R$ 3.000,00')).toBeVisible()
  await expect(page.getByRole('link', { name: /401 · macho/ })).toBeVisible()

  // Venda da 303 e cancelamento
  await page.goto('/pecuaria?aba=compras-e-vendas')
  await page.getByRole('tab', { name: 'Vender animais' }).click()
  await page.getByLabel('Cliente').selectOption({ label: 'Frigorífico Norte' })
  await page.getByRole('checkbox', { name: /^303/ }).check()
  await page.getByLabel('Valor total (R$)').fill('4500')
  await page.getByRole('button', { name: 'Registrar venda' }).click()
  await expect(page.getByRole('main').getByRole('status')).toHaveText('Venda de 1 cabeça registrada.')
  await expect(page.getByText('3 cabeças de Bovino')).toBeVisible()

  await page.getByRole('button', { name: /^Cancelar venda de/ }).click()
  await page.getByRole('alertdialog').getByRole('button', { name: 'Cancelar venda' }).click()
  await expect(page.getByText('4 cabeças de Bovino')).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: 'Frigorífico Norte' })).toContainText('cancelada')
})

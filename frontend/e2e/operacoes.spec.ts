import { expect, test } from '@playwright/test'
import { api, createOwner, signIn } from './support'

test('compra entra no estoque e venda dá baixa, com registro no histórico', async ({ page, request }) => {
  const owner = await createOwner(request, 'Operações')
  const product = await api<{ id: number }>(request, owner, 'post', '/inventory/products', {
    name: 'Ração',
    unit: 'kg',
    minStock: 0,
  })
  await api(request, owner, 'post', '/purchases/suppliers', { name: 'Agropecuária Central' })
  await api(request, owner, 'post', '/sales/customers', { name: 'Laticínio do Vale' })

  await signIn(page, owner, '/compras')
  const purchase = page.locator('form').filter({ has: page.getByRole('heading', { name: 'Nova compra' }) })
  await purchase.getByLabel('Fornecedor').selectOption({ label: 'Agropecuária Central' })
  await purchase.getByLabel('Produto do item 1').selectOption(String(product.id))
  await purchase.getByLabel(/Qtd/).fill('10')
  await purchase.getByLabel('Preço unit.').fill('5')
  await purchase.getByRole('button', { name: 'Registrar compra' }).click()
  await expect(page.getByRole('main').getByRole('status')).toBeVisible()

  await page.getByRole('link', { name: 'Vendas' }).click()
  const sale = page.locator('form').filter({ has: page.getByRole('heading', { name: 'Nova venda' }) })
  await sale.getByLabel('Cliente').selectOption({ label: 'Laticínio do Vale' })
  await sale.getByLabel('Produto do item 1').selectOption(String(product.id))
  await sale.getByLabel(/Qtd/).fill('4')
  await sale.getByLabel('Preço unit.').fill('9')
  await sale.getByRole('button', { name: 'Registrar venda' }).click()
  await expect(page.getByRole('main').getByRole('status')).toBeVisible()

  await page.getByRole('link', { name: 'Estoque' }).click()
  const products = page.getByRole('table').filter({ has: page.getByRole('columnheader', { name: 'Custo médio' }) })
  const row = products.getByRole('row').filter({ hasText: 'Ração' })
  await expect(row).toContainText('6 kg')
  await expect(row).toContainText('R$ 5,00') // custo médio

  await page.getByRole('link', { name: 'Histórico' }).click()
  await expect(page.getByRole('row').filter({ hasText: 'Criou venda' })).toContainText('Operações')
  await expect(page.getByRole('row').filter({ hasText: 'Criou compra' })).toContainText('R$ 50,00')
})

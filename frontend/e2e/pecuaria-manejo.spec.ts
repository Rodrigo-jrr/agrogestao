import { expect, test } from '@playwright/test'
import { api, createOwner, signIn } from './support'

function isoDay(offset = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

test('manejo: vacina do estoque no lote, agenda, carência, pesagem e desfazer', async ({ page, request }) => {
  const owner = await createOwner(request, 'Sanitarista')
  const species = await api<{ id: number }>(request, owner, 'post', '/livestock/species', { name: 'Bovino', allowsIndividual: true })
  const batch = await api<{ id: number }>(request, owner, 'post', '/livestock/batches', {
    name: 'Recria',
    speciesId: species.id,
    tracking: 'INDIVIDUAL',
  })
  for (const tag of ['R1', 'R2']) {
    await api(request, owner, 'post', '/livestock/animals', { tag, speciesId: species.id, sex: 'MACHO', batchId: batch.id, weight: 200 })
  }
  const vaccine = await api<{ id: number }>(request, owner, 'post', '/inventory/products', { name: 'Aftosa', unit: 'mL', minStock: 0 })
  await api(request, owner, 'post', '/inventory/movements', {
    productId: vaccine.id,
    type: 'IN',
    quantity: 50,
    unitCost: 1,
    movementDate: isoDay(),
  })

  // Vacina no lote inteiro, com produto do estoque, carência e reforço
  await signIn(page, owner, '/pecuaria?aba=sanidade')
  const form = page.locator('#registrar-sanidade')
  await form.getByLabel('Lote', { exact: true }).selectOption({ label: 'Recria (bovino, 2 cab.)' })
  await form.getByLabel('Produto do estoque').selectOption({ label: 'Aftosa (50 mL)' })
  await form.getByLabel(/Dose por cabeça/).fill('5')
  await expect(form.getByText('Sai do estoque: 10 mL (≈ R$ 10,00)')).toBeVisible()
  await form.getByLabel('Carência (dias)').fill('30')
  await form.getByLabel(/Próxima data/).fill(isoDay(3))
  await form.getByLabel('O que fazer nela').fill('Reforço')
  await form.getByRole('button', { name: 'Registrar vacina' }).click()
  await expect(form.getByRole('status')).toHaveText('Vacina registrada para lote Recria (R$ 10,00).')

  // Agenda: reforço em 3 dias e os dois animais em carência
  await expect(page.getByText('Reforço — lote Recria')).toBeVisible()
  await expect(page.getByText('em 3 dias')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Animal R1' })).toBeVisible()

  // Pesagem no tronco
  await page.getByRole('tab', { name: 'Pesagens' }).click()
  // A troca de aba é uma transição: espera a aba nova antes de preencher (o formulário de sanidade também tem "Lote").
  await expect(page.getByRole('heading', { name: 'Registrar pesagem' })).toBeVisible()
  const lote = page.getByLabel('Lote', { exact: true })
  await lote.selectOption({ label: 'Recria (bovino, 2 cab.)' })
  await expect(lote).not.toHaveValue('')
  await expect(page.getByLabel('Peso de R2')).toBeVisible()
  await page.getByLabel('Peso de R1').fill('231.5')
  await page.getByRole('button', { name: 'Salvar pesagem' }).click()
  await expect(page.getByRole('main').getByRole('status')).toHaveText('1 pesagem registrada.')

  // Ficha: carência, peso novo e vacina na linha do tempo
  await page.getByRole('link', { name: 'R1' }).click()
  await expect(page.getByText(/carência até/).first()).toBeVisible()
  await expect(page.getByText('231,5', { exact: false }).first()).toBeVisible()
  await expect(page.getByText(/Aftosa \(5 mL\) · R\$\s5,00/)).toBeVisible()

  // Painel mostra o alerta
  await page.getByRole('link', { name: 'Dashboard' }).click()
  await expect(page.getByRole('heading', { name: /Pecuária/ })).toBeVisible()
  await expect(page.getByText('Reforço — lote Recria')).toBeVisible()

  // Desfazer devolve a vacina ao estoque e limpa a agenda
  await page.goto('/pecuaria?aba=sanidade')
  await page.getByRole('button', { name: /^Desfazer vacina Aftosa/ }).click()
  await page.getByRole('alertdialog').getByRole('button', { name: 'Desfazer aplicação' }).click()
  await expect(page.getByText('Nada pendente')).toBeVisible()
  const products = await api<{ name: string; currentQuantity: number }[]>(request, owner, 'get', '/inventory/products')
  expect(products.find((p) => p.name === 'Aftosa')?.currentQuantity).toBe(50)
})

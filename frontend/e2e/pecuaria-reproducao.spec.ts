import { expect, test } from '@playwright/test'
import { api, createOwner, signIn } from './support'

function isoDay(offset = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

test('reprodução: cio, cobertura, diagnóstico de prenhez, parto com cria e parto sem diagnóstico falha', async ({ page, request }) => {
  const owner = await createOwner(request, 'Criador')
  const species = await api<{ id: number }>(request, owner, 'post', '/livestock/species', { name: 'Bovino', allowsIndividual: true })
  const mother = await api<{ id: number }>(request, owner, 'post', '/livestock/animals', { tag: '100', speciesId: species.id, sex: 'FEMEA' })

  await signIn(page, owner, `/pecuaria/animais/${mother.id}`)
  const form = page.locator('#reproducao')
  await expect(form.getByText('Vazia', { exact: true })).toBeVisible()

  // Cio: não muda a situação
  await form.getByRole('button', { name: /^Registrar cio$/ }).click()
  await expect(form.getByRole('status')).toHaveText('Cio registrado.')
  await expect(form.getByText('Vazia', { exact: true })).toBeVisible()

  // Cobertura: fêmea passa a "coberta"
  await form.locator('label').filter({ hasText: /^Cobertura$/ }).click()
  await form.getByLabel(/Touro, reprodutor ou sêmen/).fill('Touro 7')
  await form.getByRole('button', { name: /^Registrar cobertura$/ }).click()
  await expect(form.getByRole('status')).toHaveText('Cobertura registrada.')
  await expect(form.getByText('Coberta', { exact: true })).toBeVisible()

  // Diagnóstico de prenhez positivo: fêmea passa a "prenhe", com previsão de parto
  await form.locator('label').filter({ hasText: /^Diagnóstico de prenhez$/ }).click()
  await form.locator('label').filter({ hasText: /^Positivo$/ }).click()
  await form.getByLabel('Previsão do parto').fill(isoDay(270))
  await form.getByRole('button', { name: /^Registrar diagnóstico de prenhez$/ }).click()
  await expect(form.getByRole('status')).toHaveText('Diagnóstico de prenhez registrado.')
  await expect(form.getByText('Prenhe', { exact: true })).toBeVisible()
  await expect(form.getByText(/parto previsto para/)).toBeVisible()

  // Parto com uma cria: a cria entra no rebanho e a fêmea volta a "vazia"
  await form.locator('label').filter({ hasText: /^Parto$/ }).click()
  await form.getByLabel('Brinco da cria 1').fill('500')
  await form.getByLabel('Sexo da cria 1').selectOption('MACHO')
  await form.getByRole('button', { name: /^Registrar parto$/ }).click()
  await expect(form.getByRole('status')).toHaveText('Parto registrado.')
  await expect(form.getByText('Vazia', { exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: /500 · macho/ })).toBeVisible()

  // Novo parto sem diagnóstico de prenhez em aberto: o servidor recusa
  await form.getByLabel('Brinco da cria 1').fill('501')
  await form.getByRole('button', { name: /^Registrar parto$/ }).click()
  await expect(form.getByRole('alert')).toContainText('não tem diagnóstico de prenhez positivo em aberto')
})

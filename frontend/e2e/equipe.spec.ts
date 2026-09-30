import { expect, test } from '@playwright/test'
import { PASSWORD, createOwner, emailLink, signIn, uniqueEmail } from './support'

test('convite por e-mail: a pessoa cria a conta pelo link e entra na equipe', async ({ page, request, browser }) => {
  const owner = await createOwner(request, 'Convidante')
  const invitedEmail = uniqueEmail('convidada')

  await signIn(page, owner, '/equipe')
  await page.getByLabel('E-mail da pessoa').fill(invitedEmail)
  await page.getByLabel('Papel').selectOption('WORKER')
  await page.getByRole('button', { name: 'Enviar convite' }).click()
  await expect(page.getByRole('main').getByRole('status')).toContainText(`Convite enviado para ${invitedEmail}`)
  await expect(page.getByRole('main').getByRole('list')).toContainText(invitedEmail)

  // A pessoa convidada abre o link em outro navegador, sem conta.
  const link = await emailLink(request, invitedEmail, '/convite')
  const guest = await browser.newContext()
  const guestPage = await guest.newPage()
  await guestPage.goto(link.replace(/^https?:\/\/[^/]+/, ''))
  await expect(guestPage.getByText(`Convidante convidou você para a equipe de ${owner.propertyName}`)).toBeVisible()
  await expect(guestPage.getByLabel('E-mail')).toHaveValue(invitedEmail)
  await guestPage.getByLabel('Seu nome').fill('Carla Convidada')
  await guestPage.getByLabel('Crie uma senha').fill(PASSWORD)
  await guestPage.getByRole('checkbox', { name: /Termos de uso/ }).check()
  await guestPage.getByRole('button', { name: 'Criar conta e entrar na equipe' }).click()

  await expect(guestPage).toHaveURL(/\/dashboard$/)
  await expect(guestPage.getByRole('banner').getByRole('combobox').locator('option:checked')).toHaveText(owner.propertyName)
  // Colaborador não vê o histórico.
  await expect(guestPage.getByRole('link', { name: 'Histórico' })).toHaveCount(0)
  await guest.close()

  await page.reload()
  await expect(page.getByRole('row').filter({ hasText: 'Carla Convidada' })).toBeVisible()
  await expect(page.getByText('Convites pendentes')).toHaveCount(0)
})

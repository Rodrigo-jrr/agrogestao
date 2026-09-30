import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { TERMS_VERSION } from '../src/lib/legal'

const MAILPIT = process.env.MAILPIT_URL ?? 'http://localhost:8025'
export const PASSWORD = 'senhaE2e2026'

export interface Session {
  token: string
  userId: number
  name: string
  email: string
  propertyId: number
  propertyName: string
}

let sequence = 0

/** E-mail único por execução, para os testes não dependerem do que já está no banco. */
export function uniqueEmail(prefix: string) {
  sequence += 1
  return `${prefix}.${Date.now()}${sequence}@e2e.teste.dev`
}

export async function api<T = unknown>(
  request: APIRequestContext,
  session: Pick<Session, 'token'> & Partial<Pick<Session, 'propertyId'>> | null,
  method: 'get' | 'post' | 'put' | 'delete',
  path: string,
  data?: unknown,
): Promise<T> {
  const headers: Record<string, string> = {}
  if (session) {
    headers.Authorization = `Bearer ${session.token}`
    if (session.propertyId) headers['X-Property-Id'] = String(session.propertyId)
  }
  const response = await request[method](`/api${path}`, { headers, data })
  expect(response.ok(), `${method.toUpperCase()} ${path} -> ${response.status()} ${await response.text()}`).toBeTruthy()
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Conta nova com uma propriedade (pela API, para ir direto ao que cada teste verifica). */
export async function createOwner(request: APIRequestContext, name: string): Promise<Session> {
  const email = uniqueEmail(name.toLowerCase().split(' ')[0])
  const auth = await api<{ token: string; userId: number; name: string }>(request, null, 'post', '/auth/register', {
    name,
    email,
    password: PASSWORD,
    termsVersion: TERMS_VERSION,
  })
  const propertyName = `Sítio ${name}`
  const property = await api<{ id: number }>(request, { token: auth.token }, 'post', '/onboarding', {
    organizationName: `Org ${name}`,
    propertyName,
    activities: [{ name: 'Leite', type: 'LEITE' }],
  })
  return { token: auth.token, userId: auth.userId, name, email, propertyId: property.id, propertyName }
}

/** Entra no app com a sessão (como o login faria) e abre a página. */
export async function signIn(page: Page, session: Session, path = '/dashboard') {
  await page.goto('/login')
  await page.evaluate((s) => {
    localStorage.setItem('agro.token', s.token)
    localStorage.setItem('agro.user', JSON.stringify({ userId: s.userId, name: s.name, email: s.email }))
    localStorage.setItem('agro.propertyId', String(s.propertyId))
  }, session)
  await page.goto(path)
}

/** Link do último e-mail enviado para {@code to} que contém {@code path} (espera até chegar). */
export async function emailLink(request: APIRequestContext, to: string, path: string): Promise<string> {
  let link: string | undefined
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`)
        const { messages } = (await search.json()) as { messages: { ID: string }[] }
        for (const message of messages ?? []) {
          const detail = (await (await request.get(`${MAILPIT}/api/v1/message/${message.ID}`)).json()) as { Text: string }
          const found = detail.Text.match(new RegExp(`https?://\\S+${path}\\?token=[\\w-]+`))
          if (found) {
            link = found[0]
            return true
          }
        }
        return false
      },
      { message: `e-mail com ${path} para ${to}`, timeout: 15_000 },
    )
    .toBe(true)
  return link!
}

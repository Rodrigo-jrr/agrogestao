import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { apiClient } from './client'

const originalAdapter = apiClient.defaults.adapter

/** Toda requisição responde 401, como o servidor faria. */
function respond401() {
  apiClient.defaults.adapter = (config: InternalAxiosRequestConfig) =>
    Promise.reject(
      new AxiosError('401', 'ERR_BAD_REQUEST', config, undefined, {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: { message: 'Credenciais inválidas' },
      }),
    )
}

describe('apiClient em respostas 401', () => {
  beforeEach(() => {
    localStorage.setItem('agro.token', 'token-antigo')
    respond401()
  })
  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
  })

  it('no login (senha errada) mantém a tela para mostrar a mensagem', async () => {
    await expect(apiClient.post('/auth/login', {})).rejects.toThrow()
    expect(localStorage.getItem('agro.token')).toBe('token-antigo')
  })

  it('em rota autenticada trata como sessão expirada e limpa o token', async () => {
    await expect(apiClient.get('/me')).rejects.toThrow()
    expect(localStorage.getItem('agro.token')).toBeNull()
  })
})

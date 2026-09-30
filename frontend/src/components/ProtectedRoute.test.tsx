import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TERMS_VERSION } from '../lib/legal'
import { ProtectedRoute } from './ProtectedRoute'

const auth = {
  isAuthenticated: true,
  termsAccepted: true as boolean | null,
  refreshMe: vi.fn(),
  logout: vi.fn(),
}
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth }))

const acceptTerms = vi.fn()
vi.mock('../api/endpoints', () => ({ meApi: { acceptTerms: (v: string) => acceptTerms(v) } }))

function setup() {
  render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/login" element={<p>tela de login</p>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<p>painel</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    auth.isAuthenticated = true
    auth.termsAccepted = true
    acceptTerms.mockReset()
  })

  it('manda para o login quem não está conectado', () => {
    auth.isAuthenticated = false
    setup()
    expect(screen.getByText('tela de login')).toBeInTheDocument()
  })

  it('libera o app para quem aceitou os termos (e enquanto o aceite ainda carrega)', () => {
    setup()
    expect(screen.getByText('painel')).toBeInTheDocument()
    auth.termsAccepted = null
    setup()
    expect(screen.getAllByText('painel')).toHaveLength(2)
  })

  it('pede o aceite da versão atual antes de continuar', async () => {
    auth.termsAccepted = false
    acceptTerms.mockResolvedValue({})
    setup()

    expect(screen.getByRole('heading', { name: 'Termos atualizados' })).toBeInTheDocument()
    expect(screen.queryByText('painel')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Aceitar e continuar' }))
    expect(acceptTerms).not.toHaveBeenCalled() // caixa ainda desmarcada

    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Aceitar e continuar' }))
    expect(acceptTerms).toHaveBeenCalledWith(TERMS_VERSION)
    expect(auth.refreshMe).toHaveBeenCalled()
  })
})

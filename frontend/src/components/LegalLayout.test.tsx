import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { TermsPage } from '../pages/TermsPage'

const useAuth = vi.fn()
vi.mock('../auth/AuthContext', () => ({ useAuth: () => useAuth() }))

function setup(url = '/termos') {
  render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/termos" element={<TermsPage />} />
        <Route path="/login" element={<p>tela de login</p>} />
        <Route path="/dashboard" element={<p>painel</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LegalLayout (via TermsPage)', () => {
  it('sem sessão: oferece caminho de volta para o login', async () => {
    useAuth.mockReturnValue({ isAuthenticated: false })
    setup()
    const back = screen.getByRole('link', { name: '← Voltar para o login' })
    expect(back).toHaveAttribute('href', '/login')
    await userEvent.click(back)
    expect(await screen.findByText('tela de login')).toBeInTheDocument()
  })

  it('com sessão: volta para o painel', async () => {
    useAuth.mockReturnValue({ isAuthenticated: true })
    setup()
    const back = screen.getByRole('link', { name: '← Voltar' })
    expect(back).toHaveAttribute('href', '/dashboard')
    await userEvent.click(back)
    expect(await screen.findByText('painel')).toBeInTheDocument()
  })
})

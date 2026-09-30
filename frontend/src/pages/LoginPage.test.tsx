import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiError } from '../test/utils'
import { LoginPage } from './LoginPage'

const login = vi.fn()
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ login }) }))

function setup(url = '/login') {
  render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<p>painel</p>} />
        <Route path="/convite" element={<p>página do convite</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function submit() {
  await userEvent.type(screen.getByLabelText('E-mail'), 'ana@teste.dev')
  await userEvent.type(screen.getByLabelText('Senha'), 'senha1234')
  await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset()
  })

  it('volta para a página de origem (?next=) depois de entrar', async () => {
    login.mockResolvedValue(undefined)
    setup('/login?next=/convite')
    await submit()
    expect(await screen.findByText('página do convite')).toBeInTheDocument()
  })

  it('ignora ?next= que aponta para outro site', async () => {
    login.mockResolvedValue(undefined)
    setup('/login?next=//malicioso.example')
    await submit()
    expect(await screen.findByText('painel')).toBeInTheDocument()
  })

  it('mostra o erro do servidor', async () => {
    login.mockImplementation(() => Promise.reject(apiError('Muitas tentativas de login. Tente novamente em 15 minutos.', 429)))
    setup()
    await submit()
    expect(await screen.findByText(/Muitas tentativas de login/)).toBeInTheDocument()
  })

  it('confirma a exclusão da conta uma única vez', () => {
    sessionStorage.setItem('agro.accountDeleted', '1')
    setup()
    expect(screen.getByRole('status')).toHaveTextContent('Sua conta foi excluída')
    expect(sessionStorage.getItem('agro.accountDeleted')).toBeNull()
  })
})

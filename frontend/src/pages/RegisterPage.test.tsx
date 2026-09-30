import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TERMS_VERSION } from '../lib/legal'
import { apiError } from '../test/utils'
import { RegisterPage } from './RegisterPage'

const register = vi.fn()
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ register }) }))

function setup() {
  render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/onboarding" element={<p>tela de onboarding</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function fillForm() {
  await userEvent.type(screen.getByLabelText('Nome'), 'Maria')
  await userEvent.type(screen.getByLabelText('E-mail'), 'maria@teste.dev')
  await userEvent.type(screen.getByLabelText('Senha'), 'senha1234')
}

describe('RegisterPage', () => {
  beforeEach(() => {
    register.mockReset()
  })

  it('não cadastra sem aceitar os termos', async () => {
    setup()
    await fillForm()
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(register).not.toHaveBeenCalled()
    expect(screen.getByRole('checkbox', { name: /Termos de uso/ })).toBeInvalid()
  })

  it('envia a versão dos termos aceita e segue para o onboarding', async () => {
    register.mockResolvedValue(undefined)
    setup()
    await fillForm()
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(register).toHaveBeenCalledWith('Maria', 'maria@teste.dev', 'senha1234', TERMS_VERSION)
    expect(await screen.findByText('tela de onboarding')).toBeInTheDocument()
  })

  it('mostra o motivo quando o cadastro é recusado', async () => {
    register.mockImplementation(() => Promise.reject(apiError('Já existe uma conta com este e-mail')))
    setup()
    await fillForm()
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText('Já existe uma conta com este e-mail')).toBeInTheDocument()
  })
})

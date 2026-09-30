import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { InvitationInfo, PropertyMemberInfo, PropertyRole } from '../api/types'
import { ConfirmProvider } from '../components/ConfirmDialog'
import { MembersPage } from './MembersPage'

const property = { id: 6, organizationId: 5, name: 'Sítio Teste', location: null, totalAreaHa: null, role: 'OWNER' as PropertyRole }
vi.mock('../property/PropertyContext', () => ({ useProperty: () => ({ currentProperty: property }) }))

const me: PropertyMemberInfo = { id: 1, userId: 1, name: 'Ana', email: 'ana@teste.dev', role: 'OWNER', currentUser: true }
const pending: InvitationInfo = {
  id: 9,
  email: 'novo@teste.dev',
  role: 'WORKER',
  invitedByName: 'Ana',
  createdAt: '2026-09-30T10:00:00',
  expiresAt: '2026-10-07T10:00:00',
  expired: false,
}

const api = vi.hoisted(() => ({
  membersList: vi.fn(),
  invitationsList: vi.fn(),
  invite: vi.fn(),
  cancel: vi.fn(),
}))
vi.mock('../api/endpoints', () => ({
  membersApi: { list: api.membersList },
  invitationsApi: { list: api.invitationsList, invite: api.invite, cancel: api.cancel },
}))

function setup() {
  render(
    <ConfirmProvider>
      <MembersPage />
    </ConfirmProvider>,
  )
}

describe('MembersPage', () => {
  beforeEach(() => {
    property.role = 'OWNER'
    api.membersList.mockResolvedValue([me])
    api.invitationsList.mockResolvedValue([])
    api.invite.mockReset()
    api.cancel.mockReset()
  })

  it('convida por e-mail quem não tem conta e mostra o convite pendente', async () => {
    api.invite.mockResolvedValue({ outcome: 'INVITED', member: null, invitation: pending })
    setup()
    expect(await screen.findByText('ana@teste.dev')).toBeInTheDocument()

    api.invitationsList.mockResolvedValue([pending])
    await userEvent.type(screen.getByLabelText('E-mail da pessoa'), 'novo@teste.dev')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(api.invite).toHaveBeenCalledWith({ email: 'novo@teste.dev', role: 'WORKER' })
    expect(await screen.findByRole('status')).toHaveTextContent('Convite enviado para novo@teste.dev')
    const list = await screen.findByRole('list')
    expect(within(list).getByText('novo@teste.dev')).toBeInTheDocument()
    expect(within(list).getByText(/vale até 07\/10\/2026/)).toBeInTheDocument()
  })

  it('avisa quando a pessoa já tinha conta e entrou direto', async () => {
    api.invite.mockResolvedValue({
      outcome: 'ADDED',
      member: { ...me, id: 2, name: 'Beto', currentUser: false },
      invitation: null,
    })
    setup()
    await userEvent.type(await screen.findByLabelText('E-mail da pessoa'), 'beto@teste.dev')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar convite' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Beto já tinha conta e entrou na equipe')
  })

  it('cancela um convite só depois de confirmar', async () => {
    api.invitationsList.mockResolvedValue([pending])
    api.cancel.mockResolvedValue(undefined)
    setup()
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    expect(api.cancel).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar convite' }))
    expect(api.cancel).toHaveBeenCalledWith(9)
  })

  it('quem só visualiza não vê o formulário de convite', async () => {
    property.role = 'VIEWER'
    setup()
    expect(await screen.findByText(/Apenas proprietários e gerentes podem gerenciar a equipe/)).toBeInTheDocument()
    expect(screen.queryByLabelText('E-mail da pessoa')).not.toBeInTheDocument()
    expect(api.invitationsList).not.toHaveBeenCalled()
  })
})

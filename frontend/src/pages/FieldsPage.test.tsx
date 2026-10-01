import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Field, PropertyRole } from '../api/types'
import { ConfirmProvider } from '../components/ConfirmDialog'
import { FieldsPage } from './FieldsPage'

const property = { id: 6, organizationId: 5, name: 'Sítio Teste', location: null, totalAreaHa: null, role: 'OWNER' as PropertyRole }
vi.mock('../property/PropertyContext', () => ({ useProperty: () => ({ currentProperty: property }) }))

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 12.5, location: 'Fundo', soilType: 'Argiloso', archived: false }

const api = vi.hoisted(() => ({
  list: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
}))
vi.mock('../api/endpoints', () => ({ fieldsApi: api }))

function setup() {
  render(
    <ConfirmProvider>
      <FieldsPage />
    </ConfirmProvider>,
  )
}

describe('FieldsPage', () => {
  beforeEach(() => {
    property.role = 'OWNER'
    api.list.mockResolvedValue([talhao])
    api.create.mockReset()
    api.update.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
  })

  it('lista os talhões já cadastrados', async () => {
    setup()
    expect(await screen.findByText('Talhão 1')).toBeInTheDocument()
    expect(screen.getByText('12,5')).toBeInTheDocument()
    expect(screen.getByText('Fundo · Argiloso')).toBeInTheDocument()
  })

  it('cadastra um talhão novo', async () => {
    api.create.mockResolvedValue({ ...talhao, id: 2, name: 'Talhão 2', areaHa: 8, location: null, soilType: null })
    setup()
    await screen.findByText('Talhão 1')
    await userEvent.type(screen.getByLabelText('Nome do talhão'), 'Talhão 2')
    await userEvent.type(screen.getByLabelText('Área (ha)'), '8')
    await userEvent.click(screen.getByRole('button', { name: 'Criar talhão' }))

    expect(api.create).toHaveBeenCalledWith({ name: 'Talhão 2', areaHa: 8, location: null, soilType: null })
  })

  it('não envia sem preencher a área, que é obrigatória', async () => {
    setup()
    await screen.findByText('Talhão 1')
    await userEvent.type(screen.getByLabelText('Nome do talhão'), 'Sem área')
    await userEvent.click(screen.getByRole('button', { name: 'Criar talhão' }))

    expect(api.create).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Área (ha)')).toBeInvalid()
  })

  it('arquiva um talhão', async () => {
    api.archive.mockResolvedValue({ ...talhao, archived: true })
    setup()
    await userEvent.click(await screen.findByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('quem só visualiza não vê o formulário nem ações de escrita', async () => {
    property.role = 'VIEWER'
    setup()
    expect(await screen.findByText('Talhão 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome do talhão')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
  })
})

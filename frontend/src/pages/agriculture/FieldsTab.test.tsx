import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Field } from '../../api/types'
import { FieldsTab } from './FieldsTab'

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 12.5, location: 'Fundo', soilType: 'Argiloso', archived: false }

const api = vi.hoisted(() => ({
  create: vi.fn(),
  update: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({ fieldsApi: api }))

function setup(fields: Field[] = [talhao], props: Partial<{ canWrite: boolean; canManage: boolean }> = {}) {
  const onChanged = vi.fn()
  render(<FieldsTab fields={fields} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />)
  return { onChanged }
}

describe('FieldsTab', () => {
  beforeEach(() => {
    api.create.mockReset()
    api.update.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
  })

  it('lista os talhões já cadastrados', () => {
    setup()
    expect(screen.getByText('Talhão 1')).toBeInTheDocument()
    expect(screen.getByText('12,5')).toBeInTheDocument()
    expect(screen.getByText('Fundo · Argiloso')).toBeInTheDocument()
  })

  it('cadastra um talhão novo', async () => {
    api.create.mockResolvedValue({ ...talhao, id: 2, name: 'Talhão 2', areaHa: 8, location: null, soilType: null })
    const { onChanged } = setup()
    await userEvent.type(screen.getByLabelText('Nome do talhão'), 'Talhão 2')
    await userEvent.type(screen.getByLabelText('Área (ha)'), '8')
    await userEvent.click(screen.getByRole('button', { name: 'Criar talhão' }))

    expect(api.create).toHaveBeenCalledWith({ name: 'Talhão 2', areaHa: 8, location: null, soilType: null })
    expect(onChanged).toHaveBeenCalled()
  })

  it('não envia sem preencher a área, que é obrigatória', async () => {
    setup()
    await userEvent.type(screen.getByLabelText('Nome do talhão'), 'Sem área')
    await userEvent.click(screen.getByRole('button', { name: 'Criar talhão' }))

    expect(api.create).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Área (ha)')).toBeInvalid()
  })

  it('arquiva um talhão', async () => {
    api.archive.mockResolvedValue({ ...talhao, archived: true })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('quem só visualiza não vê o formulário nem ações de escrita', () => {
    setup([talhao], { canWrite: false, canManage: false })
    expect(screen.getByText('Talhão 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome do talhão')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
  })
})

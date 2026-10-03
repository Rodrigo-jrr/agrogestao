import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Machine, MachineType } from '../../api/types'
import { ConfirmProvider } from '../../components/ConfirmDialog'
import { MachinesTab } from './MachinesTab'

const trator: MachineType = { id: 1, name: 'Trator', archived: false }
const trator1: Machine = {
  id: 1,
  name: 'Trator 1',
  machineTypeId: 1,
  machineTypeName: 'Trator',
  identification: 'ABC-1234',
  measurementType: 'HOROMETRO',
  currentReading: 120,
  archived: false,
}

const api = vi.hoisted(() => ({
  create: vi.fn(),
  update: vi.fn(),
  updateReading: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
  remove: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({ machinesApi: api }))

function setup(
  machines: Machine[] = [trator1],
  types: MachineType[] = [trator],
  props: Partial<{ canWrite: boolean; canManage: boolean }> = {},
) {
  const onChanged = vi.fn()
  render(
    <ConfirmProvider>
      <MachinesTab machines={machines} types={types} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />
    </ConfirmProvider>,
  )
  return { onChanged }
}

describe('MachinesTab', () => {
  beforeEach(() => {
    api.create.mockReset()
    api.update.mockReset()
    api.updateReading.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
    api.remove.mockReset()
  })

  it('lista as máquinas já cadastradas', () => {
    setup()
    expect(screen.getByText('Trator 1')).toBeInTheDocument()
    expect(screen.getByText('Trator · ABC-1234')).toBeInTheDocument()
    expect(screen.getByText('120')).toBeInTheDocument()
    expect(screen.getByText('h')).toBeInTheDocument()
  })

  it('cadastra uma máquina nova', async () => {
    api.create.mockResolvedValue({ ...trator1, id: 2, name: 'Trator 2' })
    const { onChanged } = setup()
    await userEvent.type(screen.getByLabelText('Nome'), 'Trator 2')
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'Trator')
    await userEvent.type(screen.getByLabelText(/Leitura inicial/), '50')
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar máquina' }))

    expect(api.create).toHaveBeenCalledWith({
      name: 'Trator 2',
      machineTypeId: 1,
      identification: null,
      measurementType: 'HOROMETRO',
      currentReading: 50,
    })
    expect(onChanged).toHaveBeenCalled()
  })

  it('edita o cadastro sem alterar a leitura', async () => {
    api.update.mockResolvedValue({ ...trator1, name: 'Trator 1 renomeado' })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Editar' }))
    const nameInput = within(screen.getByRole('list')).getByLabelText('Nome')
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Trator 1 renomeado')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    expect(api.update).toHaveBeenCalledWith(1, {
      name: 'Trator 1 renomeado',
      machineTypeId: 1,
      identification: 'ABC-1234',
      measurementType: 'HOROMETRO',
    })
  })

  it('registra uma leitura maior que a atual', async () => {
    api.updateReading.mockResolvedValue({ ...trator1, currentReading: 150 })
    const { onChanged } = setup()
    await userEvent.click(screen.getByRole('button', { name: 'Registrar leitura' }))
    const readingInput = screen.getByLabelText('Nova leitura')
    await userEvent.clear(readingInput)
    await userEvent.type(readingInput, '150')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar leitura' }))

    expect(api.updateReading).toHaveBeenCalledWith(1, { reading: 150 })
    expect(onChanged).toHaveBeenCalled()
  })

  it('bloqueia registrar leitura menor que a atual', async () => {
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Registrar leitura' }))
    const readingInput = screen.getByLabelText('Nova leitura')
    await userEvent.clear(readingInput)
    await userEvent.type(readingInput, '100')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar leitura' }))

    expect(api.updateReading).not.toHaveBeenCalled()
    expect(screen.getByText('A leitura não pode ser menor que a atual (120).')).toBeInTheDocument()
  })

  it('arquiva uma máquina', async () => {
    api.archive.mockResolvedValue({ ...trator1, archived: true })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('exclui uma máquina depois de confirmar', async () => {
    api.remove.mockResolvedValue(undefined)
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir máquina' }))
    expect(api.remove).toHaveBeenCalledWith(1)
  })

  it('mostra o erro do servidor quando a exclusão é bloqueada por uso', async () => {
    api.remove.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'A máquina Trator 1 já tem operações registradas e não pode ser excluída.' } },
    })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir máquina' }))
    expect(await screen.findByText('A máquina Trator 1 já tem operações registradas e não pode ser excluída.')).toBeInTheDocument()
  })

  it('pede para cadastrar um tipo quando não há nenhum ativo', () => {
    setup([], [])
    expect(screen.getByText('Cadastre um tipo de máquina antes de registrar a primeira máquina.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  })

  it('quem só visualiza não vê formulário nem ações de escrita', () => {
    setup([trator1], [trator], { canWrite: false, canManage: false })
    expect(screen.getByText('Trator 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar leitura' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })
})

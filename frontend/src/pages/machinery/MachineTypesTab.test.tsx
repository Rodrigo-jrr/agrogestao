import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MachineType } from '../../api/types'
import { ConfirmProvider } from '../../components/ConfirmDialog'
import { MachineTypesTab } from './MachineTypesTab'

const trator: MachineType = { id: 1, name: 'Trator', archived: false }

const api = vi.hoisted(() => ({
  create: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
  remove: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({ machineTypesApi: api }))

function setup(types: MachineType[] = [trator], props: Partial<{ canWrite: boolean; canManage: boolean }> = {}) {
  const onChanged = vi.fn()
  render(
    <ConfirmProvider>
      <MachineTypesTab types={types} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />
    </ConfirmProvider>,
  )
  return { onChanged }
}

describe('MachineTypesTab', () => {
  beforeEach(() => {
    api.create.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
    api.remove.mockReset()
  })

  it('lista os tipos já cadastrados', () => {
    setup()
    expect(screen.getByText('Trator')).toBeInTheDocument()
  })

  it('cadastra um tipo novo', async () => {
    api.create.mockResolvedValue({ id: 2, name: 'Colheitadeira', archived: false })
    const { onChanged } = setup()
    await userEvent.type(screen.getByLabelText('Nome do tipo'), 'Colheitadeira')
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar tipo' }))

    expect(api.create).toHaveBeenCalledWith({ name: 'Colheitadeira' })
    expect(onChanged).toHaveBeenCalled()
  })

  it('arquiva um tipo', async () => {
    api.archive.mockResolvedValue({ ...trator, archived: true })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('exclui um tipo depois de confirmar', async () => {
    api.remove.mockResolvedValue(undefined)
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir tipo' }))
    expect(api.remove).toHaveBeenCalledWith(1)
  })

  it('mostra o erro do servidor quando a exclusão é bloqueada por uso', async () => {
    api.remove.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'O tipo Trator já tem máquinas cadastradas e não pode ser excluído.' } },
    })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir tipo' }))
    expect(await screen.findByText('O tipo Trator já tem máquinas cadastradas e não pode ser excluído.')).toBeInTheDocument()
  })

  it('quem só visualiza não vê o formulário nem ações de escrita', () => {
    setup([trator], { canWrite: false, canManage: false })
    expect(screen.getByText('Trator')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome do tipo')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })
})

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Crop } from '../../api/types'
import { ConfirmProvider } from '../../components/ConfirmDialog'
import { CropsTab } from './CropsTab'

const soja: Crop = { id: 1, name: 'Soja', archived: false }

const api = vi.hoisted(() => ({
  create: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
  remove: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({ cropsApi: api }))

function setup(crops: Crop[] = [soja], props: Partial<{ canWrite: boolean; canManage: boolean }> = {}) {
  const onChanged = vi.fn()
  render(
    <ConfirmProvider>
      <CropsTab crops={crops} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />
    </ConfirmProvider>,
  )
  return { onChanged }
}

describe('CropsTab', () => {
  beforeEach(() => {
    api.create.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
    api.remove.mockReset()
  })

  it('lista as culturas já cadastradas', () => {
    setup()
    expect(screen.getByText('Soja')).toBeInTheDocument()
  })

  it('cadastra uma cultura nova', async () => {
    api.create.mockResolvedValue({ id: 2, name: 'Milho', archived: false })
    const { onChanged } = setup()
    await userEvent.type(screen.getByLabelText('Nome da cultura'), 'Milho')
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar cultura' }))

    expect(api.create).toHaveBeenCalledWith({ name: 'Milho' })
    expect(onChanged).toHaveBeenCalled()
  })

  it('arquiva uma cultura', async () => {
    api.archive.mockResolvedValue({ ...soja, archived: true })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('exclui uma cultura depois de confirmar', async () => {
    api.remove.mockResolvedValue(undefined)
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir cultura' }))
    expect(api.remove).toHaveBeenCalledWith(1)
  })

  it('mostra o erro do servidor quando a exclusão é bloqueada por uso', async () => {
    api.remove.mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'A cultura Soja já tem safras cadastradas e não pode ser excluída.' } },
    })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    await userEvent.click(screen.getByRole('button', { name: 'Excluir cultura' }))
    expect(await screen.findByText('A cultura Soja já tem safras cadastradas e não pode ser excluída.')).toBeInTheDocument()
  })

  it('quem só visualiza não vê o formulário nem ações de escrita', () => {
    setup([soja], { canWrite: false, canManage: false })
    expect(screen.getByText('Soja')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome da cultura')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })
})

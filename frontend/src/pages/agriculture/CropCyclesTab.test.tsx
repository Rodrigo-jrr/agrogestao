import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Crop, CropCycle, Field } from '../../api/types'
import { CropCyclesTab } from './CropCyclesTab'

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 12.5, location: null, soilType: null, archived: false }
const soja: Crop = { id: 1, name: 'Soja', archived: false }
const safra: CropCycle = {
  id: 1,
  fieldId: 1,
  fieldName: 'Talhão 1',
  cropId: 1,
  cropName: 'Soja',
  variety: 'BRS 1010',
  plannedPlantingDate: '2026-10-15',
  plannedHarvestDate: '2027-02-15',
  archived: false,
}

const api = vi.hoisted(() => ({
  create: vi.fn(),
  archive: vi.fn(),
  unarchive: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({ cropCyclesApi: api }))

function setup(
  cycles: CropCycle[] = [safra],
  fields: Field[] = [talhao],
  crops: Crop[] = [soja],
  props: Partial<{ canWrite: boolean; canManage: boolean }> = {},
) {
  const onChanged = vi.fn()
  render(
    <MemoryRouter>
      <CropCyclesTab
        cycles={cycles}
        fields={fields}
        crops={crops}
        canWrite={props.canWrite ?? true}
        canManage={props.canManage ?? true}
        onChanged={onChanged}
      />
    </MemoryRouter>,
  )
  return { onChanged }
}

describe('CropCyclesTab', () => {
  beforeEach(() => {
    api.create.mockReset()
    api.archive.mockReset()
    api.unarchive.mockReset()
  })

  it('lista as safras já cadastradas', () => {
    setup()
    expect(screen.getByText('Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.getByText('BRS 1010 · Plantio 15/10/2026 · Colheita prevista 15/02/2027')).toBeInTheDocument()
  })

  it('cadastra uma safra nova', async () => {
    api.create.mockResolvedValue({ ...safra, id: 2 })
    const { onChanged } = setup()
    await userEvent.selectOptions(screen.getByLabelText('Talhão'), 'Talhão 1')
    await userEvent.selectOptions(screen.getByLabelText('Cultura'), 'Soja')
    await userEvent.type(screen.getByLabelText('Plantio previsto'), '2026-11-01')
    await userEvent.click(screen.getByRole('button', { name: 'Criar safra' }))

    expect(api.create).toHaveBeenCalledWith({
      fieldId: 1,
      cropId: 1,
      variety: null,
      plannedPlantingDate: '2026-11-01',
      plannedHarvestDate: null,
    })
    expect(onChanged).toHaveBeenCalled()
  })

  it('não envia quando a colheita prevista é antes do plantio', async () => {
    setup()
    await userEvent.selectOptions(screen.getByLabelText('Talhão'), 'Talhão 1')
    await userEvent.selectOptions(screen.getByLabelText('Cultura'), 'Soja')
    await userEvent.type(screen.getByLabelText('Plantio previsto'), '2026-11-01')
    await userEvent.type(screen.getByLabelText(/Colheita prevista/), '2026-10-01')
    await userEvent.click(screen.getByRole('button', { name: 'Criar safra' }))

    expect(api.create).not.toHaveBeenCalled()
    expect(screen.getByText('A data de colheita prevista não pode ser antes da data de plantio.')).toBeInTheDocument()
  })

  it('pede para cadastrar talhão/cultura quando não há nenhum ativo', () => {
    setup([], [], [])
    expect(screen.getByText('Cadastre um talhão e uma cultura para começar a registrar safras.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Talhão')).not.toBeInTheDocument()
  })

  it('arquiva uma safra', async () => {
    api.archive.mockResolvedValue({ ...safra, archived: true })
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Arquivar' }))
    expect(api.archive).toHaveBeenCalledWith(1)
  })

  it('quem só visualiza não vê o formulário nem ações de escrita', () => {
    setup([safra], [talhao], [soja], { canWrite: false, canManage: false })
    expect(screen.getByText('Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Talhão')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
  })
})

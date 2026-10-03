import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CropCycle, CropIndicators, Field } from '../../api/types'
import { CropIndicatorsTab } from './CropIndicatorsTab'

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 20, location: null, soilType: null, archived: false }
const soja: CropCycle = {
  id: 1,
  fieldId: 1,
  fieldName: 'Talhão 1',
  cropId: 1,
  cropName: 'Soja',
  variety: null,
  plannedPlantingDate: '2026-10-01',
  plannedHarvestDate: null,
  archived: false,
}

const indicators: CropIndicators = {
  cropCycleId: null,
  cropCycleLabel: null,
  fieldId: null,
  fieldName: null,
  from: '2026-10-01',
  to: '2026-10-31',
  cycleCount: 2,
  totalAreaHa: 20,
  totalCost: 1000,
  costPerHa: 50,
  harvestedQuantityByUnit: { sc: 100 },
  productionPerHaByUnit: { sc: 5 },
  revenue: 4000,
  revenuePerHa: 200,
  marginPerHa: 150,
}

const api = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../api/endpoints', () => ({ cropIndicatorsApi: { get: api.get } }))

function setup(cycles: CropCycle[] = [soja], fields: Field[] = [talhao]) {
  render(<CropIndicatorsTab cycles={cycles} fields={fields} />)
}

describe('CropIndicatorsTab', () => {
  beforeEach(() => {
    api.get.mockReset().mockResolvedValue(indicators)
  })

  it('mostra os indicadores da propriedade inteira por padrão', async () => {
    setup()
    expect(await screen.findByText('R$ 50,00')).toBeInTheDocument()
    expect(api.get).toHaveBeenCalledWith(expect.objectContaining({ cropCycleId: undefined, fieldId: undefined }))
    expect(screen.getByText('5 sc/ha')).toBeInTheDocument()
    expect(screen.getByText('R$ 1.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 4.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 200,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 150,00')).toBeInTheDocument()
  })

  it('troca o escopo para um talhão específico', async () => {
    setup()
    await screen.findByText('R$ 50,00')
    await userEvent.selectOptions(screen.getByLabelText('Escopo'), 'f:1')
    expect(api.get).toHaveBeenLastCalledWith(expect.objectContaining({ cropCycleId: undefined, fieldId: 1 }))
  })

  it('troca o escopo para uma safra específica', async () => {
    setup()
    await screen.findByText('R$ 50,00')
    await userEvent.selectOptions(screen.getByLabelText('Escopo'), 'c:1')
    expect(api.get).toHaveBeenLastCalledWith(expect.objectContaining({ cropCycleId: 1, fieldId: undefined }))
  })

  it('mostra uma produção por ha para cada unidade colhida', async () => {
    api.get.mockResolvedValue({
      ...indicators,
      harvestedQuantityByUnit: { sc: 100, kg: 50 },
      productionPerHaByUnit: { sc: 5, kg: 2.5 },
    })
    setup()
    expect(await screen.findByText('5 sc/ha')).toBeInTheDocument()
    expect(screen.getByText('2,5 kg/ha')).toBeInTheDocument()
  })

  it('mostra "sem dados" quando não há área para custo/receita/margem por ha', async () => {
    api.get.mockResolvedValue({ ...indicators, costPerHa: null, revenuePerHa: null, marginPerHa: null })
    setup()
    await screen.findByText('R$ 1.000,00')
    expect(screen.getAllByText('sem dados')).toHaveLength(3)
  })
})

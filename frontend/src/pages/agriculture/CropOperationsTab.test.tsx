import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CropCycle, CropOperation, Field, Machine, Product } from '../../api/types'
import { ConfirmProvider } from '../../components/ConfirmDialog'
import { CropOperationsTab } from './CropOperationsTab'

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 12.5, location: null, soilType: null, archived: false }
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
const ureia: Product = {
  id: 1,
  name: 'Ureia',
  category: null,
  unit: 'kg',
  minStock: 0,
  currentQuantity: 100,
  averageCost: 2,
  stockValue: 200,
  archived: false,
}
const operacao: CropOperation = {
  id: 1,
  cropCycleId: 1,
  fieldId: 1,
  fieldName: 'Talhão 1',
  cropId: 1,
  cropName: 'Soja',
  kind: 'ADUBACAO',
  operationDate: '2026-09-20',
  productId: 1,
  productName: 'Ureia',
  quantity: 10,
  quantityUnit: 'kg',
  totalCost: 20,
  responsible: 'João',
  machine: 'Trator',
  machineId: null,
  machineName: null,
  notes: null,
}
const trator: Machine = {
  id: 1,
  name: 'Trator 1',
  machineTypeId: 1,
  machineTypeName: 'Trator',
  identification: null,
  measurementType: 'HOROMETRO',
  currentReading: 500,
  archived: false,
}

const api = vi.hoisted(() => ({
  list: vi.fn(),
  create: vi.fn(),
  remove: vi.fn(),
  listProducts: vi.fn(),
  listMachines: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({
  cropOperationsApi: { list: api.list, create: api.create, remove: api.remove },
  inventoryApi: { listProducts: api.listProducts },
  machinesApi: { list: api.listMachines },
}))

function emptyPage() {
  return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
}

function setup(
  cycles: CropCycle[] = [soja],
  fields: Field[] = [talhao],
  props: Partial<{ canWrite: boolean; canManage: boolean }> = {},
) {
  const onChanged = vi.fn()
  render(
    <MemoryRouter>
      <ConfirmProvider>
        <CropOperationsTab cycles={cycles} fields={fields} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />
      </ConfirmProvider>
    </MemoryRouter>,
  )
  return { onChanged }
}

describe('CropOperationsTab', () => {
  beforeEach(() => {
    api.list.mockReset().mockResolvedValue(emptyPage())
    api.create.mockReset()
    api.remove.mockReset()
    api.listProducts.mockReset().mockResolvedValue([])
    api.listMachines.mockReset().mockResolvedValue([])
  })

  it('lista as operações já cadastradas', async () => {
    api.list.mockResolvedValue({ content: [operacao], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    setup()
    expect(await screen.findByText('Adubação · Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.getByText(/Ureia · 10 kg/)).toBeInTheDocument()
  })

  it('cadastra operação com produto do estoque', async () => {
    api.listProducts.mockResolvedValue([ureia])
    api.create.mockResolvedValue({ ...operacao, id: 2 })
    const { onChanged } = setup()
    const dateInput = (await screen.findByLabelText('Data')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.click(screen.getByText('Do estoque'))
    await screen.findByText('Ureia (100 kg)')
    await userEvent.selectOptions(screen.getByLabelText('Produto do estoque'), '1')
    await userEvent.type(screen.getByLabelText(/Quantidade usada/), '10')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar operação' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      kind: 'PREPARO',
      operationDate: today,
      productId: 1,
      quantity: 10,
      responsible: null,
      machine: null,
      notes: null,
    })
    expect(onChanged).toHaveBeenCalled()
  })

  it('cadastra operação com insumo digitado manualmente, sem produto do estoque', async () => {
    api.create.mockResolvedValue({ ...operacao, id: 3 })
    setup()
    const dateInput = (await screen.findByLabelText('Data')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.click(screen.getByText('Outro'))
    await userEvent.type(screen.getByLabelText(/Insumo usado/), 'Calcário')
    await userEvent.type(screen.getByLabelText('Quantidade (opcional)'), '500')
    await userEvent.type(screen.getByLabelText('Unidade'), 'kg')
    await userEvent.type(screen.getByLabelText(/Custo total/), '150')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar operação' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      kind: 'PREPARO',
      operationDate: today,
      productName: 'Calcário',
      quantity: 500,
      quantityUnit: 'kg',
      cost: 150,
      responsible: null,
      machine: null,
      notes: null,
    })
  })

  it('cadastra operação sem nenhum insumo', async () => {
    api.create.mockResolvedValue({ ...operacao, id: 4 })
    setup()
    const dateInput = (await screen.findByLabelText('Data')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar operação' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      kind: 'PREPARO',
      operationDate: today,
      responsible: null,
      machine: null,
      notes: null,
    })
  })

  it('cadastra operação com máquina cadastrada e leitura', async () => {
    api.listMachines.mockResolvedValue([trator])
    api.create.mockResolvedValue({ ...operacao, id: 5 })
    setup()
    const dateInput = (await screen.findByLabelText('Data')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.click(screen.getByText('Máquina cadastrada'))
    await screen.findByText('Trator 1')
    await userEvent.selectOptions(screen.getByLabelText('Máquina'), '1')
    await userEvent.type(screen.getByLabelText(/Leitura atual/), '520')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar operação' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      kind: 'PREPARO',
      operationDate: today,
      responsible: null,
      machineId: 1,
      machineReading: 520,
      notes: null,
    })
  })

  it('bloqueia quando a leitura da máquina cadastrada é menor que a atual', async () => {
    api.listMachines.mockResolvedValue([trator])
    setup()
    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.click(screen.getByText('Máquina cadastrada'))
    await screen.findByText('Trator 1')
    await userEvent.selectOptions(screen.getByLabelText('Máquina'), '1')
    await userEvent.type(screen.getByLabelText(/Leitura atual/), '100')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar operação' }))

    expect(api.create).not.toHaveBeenCalled()
    expect(screen.getByText('A leitura não pode ser menor que a atual (500).')).toBeInTheDocument()
  })

  it('mostra o nome da máquina cadastrada na lista, preferindo-o ao texto livre', async () => {
    api.list.mockResolvedValue({
      content: [{ ...operacao, machine: 'texto antigo', machineId: 1, machineName: 'Trator 1' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    setup()
    expect(await screen.findByText(/Trator 1/)).toBeInTheDocument()
    expect(screen.queryByText(/texto antigo/)).not.toBeInTheDocument()
  })

  it('pede para cadastrar uma safra ativa quando não há nenhuma', () => {
    setup([])
    expect(screen.getByText('Cadastre uma safra ativa para lançar operações.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Safra')).not.toBeInTheDocument()
  })

  it('exclui uma operação depois de confirmar, avisando que o estoque volta', async () => {
    api.list.mockResolvedValue({ content: [operacao], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    api.remove.mockResolvedValue(undefined)
    setup()
    await screen.findByText('Adubação · Soja · Talhão 1')
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    expect(screen.getByText(/Ureia volta ao estoque/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir operação' }))
    expect(api.remove).toHaveBeenCalledWith(1)
  })

  it('quem só visualiza não vê o formulário nem a exclusão', async () => {
    api.list.mockResolvedValue({ content: [operacao], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    setup([soja], [talhao], { canWrite: false, canManage: false })
    expect(await screen.findByText('Adubação · Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Safra')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })
})

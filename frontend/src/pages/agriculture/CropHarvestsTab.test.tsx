import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Customer, CropCycle, CropHarvest, Field, Product } from '../../api/types'
import { ConfirmProvider } from '../../components/ConfirmDialog'
import { CropHarvestsTab } from './CropHarvestsTab'

const talhao: Field = { id: 1, name: 'Talhão 1', areaHa: 10, location: null, soilType: null, archived: false }
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
const silo: Product = {
  id: 1,
  name: 'Soja em grão',
  category: null,
  unit: 'sc',
  minStock: 0,
  currentQuantity: 0,
  averageCost: 0,
  stockValue: 0,
  archived: false,
}
const cliente: Customer = { id: 1, name: 'Cerealista Central', document: null, phone: null, email: null }

const harvestStock: CropHarvest = {
  id: 1,
  cropCycleId: 1,
  fieldId: 1,
  fieldName: 'Talhão 1',
  cropId: 1,
  cropName: 'Soja',
  harvestDate: '2026-09-20',
  quantity: 100,
  quantityUnit: 'sc',
  destination: 'ESTOQUE',
  productId: 1,
  productName: 'Soja em grão',
  unitCost: 80,
  customerId: null,
  customerName: null,
  totalAmount: null,
  paymentType: null,
}
const harvestSale: CropHarvest = {
  id: 2,
  cropCycleId: 1,
  fieldId: 1,
  fieldName: 'Talhão 1',
  cropId: 1,
  cropName: 'Soja',
  harvestDate: '2026-09-21',
  quantity: 50,
  quantityUnit: 'sc',
  destination: 'VENDA',
  productId: null,
  productName: null,
  unitCost: null,
  customerId: 1,
  customerName: 'Cerealista Central',
  totalAmount: 4000,
  paymentType: 'INSTALLMENTS',
}

const api = vi.hoisted(() => ({
  list: vi.fn(),
  create: vi.fn(),
  remove: vi.fn(),
  listProducts: vi.fn(),
  listCustomers: vi.fn(),
  createCustomer: vi.fn(),
}))
vi.mock('../../api/endpoints', () => ({
  cropHarvestsApi: { list: api.list, create: api.create, remove: api.remove },
  inventoryApi: { listProducts: api.listProducts },
  salesApi: { listCustomers: api.listCustomers, createCustomer: api.createCustomer },
}))

function emptyPage() {
  return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
}

function setup(cycles: CropCycle[] = [soja], fields: Field[] = [talhao], props: Partial<{ canWrite: boolean; canManage: boolean }> = {}) {
  const onChanged = vi.fn()
  render(
    <MemoryRouter>
      <ConfirmProvider>
        <CropHarvestsTab cycles={cycles} fields={fields} canWrite={props.canWrite ?? true} canManage={props.canManage ?? true} onChanged={onChanged} />
      </ConfirmProvider>
    </MemoryRouter>,
  )
  return { onChanged }
}

describe('CropHarvestsTab', () => {
  beforeEach(() => {
    api.list.mockReset().mockResolvedValue(emptyPage())
    api.create.mockReset()
    api.remove.mockReset()
    api.listProducts.mockReset().mockResolvedValue([])
    api.listCustomers.mockReset().mockResolvedValue([])
    api.createCustomer.mockReset()
  })

  it('lista as colheitas já cadastradas', async () => {
    api.list.mockResolvedValue({ content: [harvestStock, harvestSale], page: 0, size: 20, totalElements: 2, totalPages: 1 })
    setup()
    expect(await screen.findByText('Estoque · Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.getByText(/Soja em grão · R\$ 80,00\/sc/)).toBeInTheDocument()
    expect(screen.getByText('Venda · Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.getByText(/Cerealista Central · R\$ 4\.000,00 · a prazo/)).toBeInTheDocument()
  })

  it('cadastra colheita para o estoque', async () => {
    api.listProducts.mockResolvedValue([silo])
    api.create.mockResolvedValue({ ...harvestStock, id: 3 })
    const { onChanged } = setup()
    const dateInput = (await screen.findByLabelText('Data da colheita')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.type(screen.getByLabelText(/Quantidade colhida/), '100')
    await screen.findByText('Soja em grão (0 sc)')
    await userEvent.selectOptions(screen.getByLabelText('Produto do estoque'), '1')
    await userEvent.type(screen.getByLabelText(/Custo unitário/), '80')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar colheita' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      harvestDate: today,
      quantity: 100,
      destination: 'ESTOQUE',
      quantityUnit: 'sc',
      productId: 1,
      unitCost: 80,
    })
    expect(onChanged).toHaveBeenCalled()
  })

  it('cadastra colheita vendida à vista', async () => {
    api.listCustomers.mockResolvedValue([cliente])
    api.create.mockResolvedValue({ ...harvestSale, id: 4, paymentType: 'CASH' })
    setup()
    const dateInput = (await screen.findByLabelText('Data da colheita')) as HTMLInputElement
    const today = dateInput.value

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.type(screen.getByLabelText(/Quantidade colhida/), '50')
    await userEvent.click(screen.getByText('Vendida'))
    await screen.findByLabelText('Cliente')
    await userEvent.selectOptions(screen.getByLabelText('Cliente'), '1')
    await userEvent.type(screen.getByLabelText('Unidade da quantidade'), 'sc')
    await userEvent.type(screen.getByLabelText('Valor total (R$)'), '4000')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar colheita' }))

    expect(api.create).toHaveBeenCalledWith({
      cropCycleId: 1,
      harvestDate: today,
      quantity: 50,
      destination: 'VENDA',
      quantityUnit: 'sc',
      customerId: 1,
      totalAmount: 4000,
      paymentType: 'CASH',
    })
  })

  it('cadastra colheita vendida a prazo gerando parcela', async () => {
    api.listCustomers.mockResolvedValue([cliente])
    api.create.mockResolvedValue({ ...harvestSale, id: 5 })
    setup()

    await userEvent.selectOptions(screen.getByLabelText('Safra'), '1')
    await userEvent.type(screen.getByLabelText(/Quantidade colhida/), '50')
    await userEvent.click(screen.getByText('Vendida'))
    await screen.findByLabelText('Cliente')
    await userEvent.selectOptions(screen.getByLabelText('Cliente'), '1')
    await userEvent.type(screen.getByLabelText('Unidade da quantidade'), 'sc')
    await userEvent.type(screen.getByLabelText('Valor total (R$)'), '4000')
    await userEvent.click(screen.getByText('A prazo'))
    await userEvent.click(screen.getByRole('button', { name: 'Registrar colheita' }))

    expect(api.create).toHaveBeenCalledTimes(1)
    const payload = api.create.mock.calls[0][0]
    expect(payload.paymentType).toBe('INSTALLMENTS')
    expect(payload.installments).toHaveLength(1)
    expect(payload.installments[0].amount).toBe(4000)
  })

  it('pede para cadastrar uma safra ativa quando não há nenhuma', () => {
    setup([])
    expect(screen.getByText('Cadastre uma safra ativa para lançar colheitas.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Safra')).not.toBeInTheDocument()
  })

  it('exclui uma colheita para o estoque, avisando que desfaz a entrada', async () => {
    api.list.mockResolvedValue({ content: [harvestStock], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    api.remove.mockResolvedValue(undefined)
    setup()
    await screen.findByText('Estoque · Soja · Talhão 1')
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    expect(screen.getByText(/Soja em grão sai do estoque pela mesma quantidade/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir colheita' }))
    expect(api.remove).toHaveBeenCalledWith(1)
  })

  it('exclui uma colheita vendida a prazo, avisando sobre as parcelas', async () => {
    api.list.mockResolvedValue({ content: [harvestSale], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    setup()
    await screen.findByText('Venda · Soja · Talhão 1')
    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    expect(screen.getByText(/As parcelas são canceladas do financeiro/)).toBeInTheDocument()
  })

  it('quem só visualiza não vê o formulário nem a exclusão', async () => {
    api.list.mockResolvedValue({ content: [harvestStock], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    setup([soja], [talhao], { canWrite: false, canManage: false })
    expect(await screen.findByText('Estoque · Soja · Talhão 1')).toBeInTheDocument()
    expect(screen.queryByLabelText('Safra')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument()
  })
})

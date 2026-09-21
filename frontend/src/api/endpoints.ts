import { apiClient } from './client'
import type {
  Activity,
  ActivityInput,
  AccountPayable,
  AccountReceivable,
  AuthResponse,
  CostCenter,
  Customer,
  DashboardSummary,
  FinancialTransaction,
  InventoryMovement,
  Product,
  ProductionRecord,
  Purchase,
  PropertySummary,
  Sale,
  Supplier,
  TransactionType,
  MovementType,
} from './types'

export const authApi = {
  register: (data: { name: string; email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),
  login: (data: { email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),
}

export const propertiesApi = {
  listMine: () => apiClient.get<PropertySummary[]>('/properties/me').then((r) => r.data),
}

export const onboardingApi = {
  create: (data: {
    organizationName: string
    propertyName: string
    location?: string
    totalAreaHa?: number
    activities: ActivityInput[]
  }) => apiClient.post<PropertySummary>('/onboarding', data).then((r) => r.data),
}

export const activitiesApi = {
  list: () => apiClient.get<Activity[]>('/activities').then((r) => r.data),
  create: (data: ActivityInput) => apiClient.post<Activity>('/activities', data).then((r) => r.data),
}

export const financeApi = {
  listTransactions: (from?: string, to?: string) =>
    apiClient.get<FinancialTransaction[]>('/finance/transactions', { params: { from, to } }).then((r) => r.data),
  createTransaction: (data: {
    type: TransactionType
    description: string
    amount: number
    transactionDate: string
    costCenterId?: number
  }) => apiClient.post<FinancialTransaction>('/finance/transactions', data).then((r) => r.data),
  listPayables: () => apiClient.get<AccountPayable[]>('/finance/payables').then((r) => r.data),
  createPayable: (data: { description: string; amount: number; dueDate: string; supplierId?: number }) =>
    apiClient.post<AccountPayable>('/finance/payables', data).then((r) => r.data),
  payPayable: (id: number) => apiClient.patch<AccountPayable>(`/finance/payables/${id}/pay`).then((r) => r.data),
  listReceivables: () => apiClient.get<AccountReceivable[]>('/finance/receivables').then((r) => r.data),
  createReceivable: (data: { description: string; amount: number; dueDate: string; customerId?: number }) =>
    apiClient.post<AccountReceivable>('/finance/receivables', data).then((r) => r.data),
  receiveReceivable: (id: number) =>
    apiClient.patch<AccountReceivable>(`/finance/receivables/${id}/receive`).then((r) => r.data),
  listCostCenters: () => apiClient.get<CostCenter[]>('/finance/cost-centers').then((r) => r.data),
  createCostCenter: (data: { name: string; activityId?: number }) =>
    apiClient.post<CostCenter>('/finance/cost-centers', data).then((r) => r.data),
}

export const inventoryApi = {
  listProducts: () => apiClient.get<Product[]>('/inventory/products').then((r) => r.data),
  createProduct: (data: { name: string; category?: string; unit: string; minStock: number }) =>
    apiClient.post<Product>('/inventory/products', data).then((r) => r.data),
  lowStock: () => apiClient.get<Product[]>('/inventory/products/low-stock').then((r) => r.data),
  listMovements: () => apiClient.get<InventoryMovement[]>('/inventory/movements').then((r) => r.data),
  createMovement: (data: {
    productId: number
    type: MovementType
    quantity: number
    unitCost?: number
    movementDate: string
    notes?: string
  }) => apiClient.post<InventoryMovement>('/inventory/movements', data).then((r) => r.data),
}

export const purchasesApi = {
  list: () => apiClient.get<Purchase[]>('/purchases').then((r) => r.data),
  create: (data: {
    supplierId: number
    purchaseDate: string
    items: { productId: number; quantity: number; unitPrice: number }[]
  }) => apiClient.post<Purchase>('/purchases', data).then((r) => r.data),
  listSuppliers: () => apiClient.get<Supplier[]>('/purchases/suppliers').then((r) => r.data),
  createSupplier: (data: { name: string; document?: string; phone?: string; email?: string }) =>
    apiClient.post<Supplier>('/purchases/suppliers', data).then((r) => r.data),
}

export const salesApi = {
  list: () => apiClient.get<Sale[]>('/sales').then((r) => r.data),
  create: (data: {
    customerId: number
    saleDate: string
    items: { productId: number; quantity: number; unitPrice: number }[]
  }) => apiClient.post<Sale>('/sales', data).then((r) => r.data),
  listCustomers: () => apiClient.get<Customer[]>('/sales/customers').then((r) => r.data),
  createCustomer: (data: { name: string; document?: string; phone?: string; email?: string }) =>
    apiClient.post<Customer>('/sales/customers', data).then((r) => r.data),
}

export const productionApi = {
  list: () => apiClient.get<ProductionRecord[]>('/production').then((r) => r.data),
  create: (data: {
    productName: string
    quantity: number
    unit: string
    productionDate: string
    activityId?: number
  }) => apiClient.post<ProductionRecord>('/production', data).then((r) => r.data),
}

export const dashboardApi = {
  summary: () => apiClient.get<DashboardSummary>('/dashboard/summary').then((r) => r.data),
}

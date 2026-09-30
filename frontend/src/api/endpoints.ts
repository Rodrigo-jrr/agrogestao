import { apiClient } from './client'
import type {
  Activity,
  ActivityInput,
  AccountPayable,
  AccountReceivable,
  AdminOverview,
  AuthResponse,
  CashFlow,
  CostCenter,
  Customer,
  DashboardSummary,
  FinancialTransaction,
  InstallmentInput,
  InventoryMovement,
  MeInfo,
  Product,
  PageResponse,
  PaymentType,
  ProductionRecord,
  PurchaseDetail,
  Purchase,
  PropertySummary,
  PropertyMemberInfo,
  PropertyRole,
  Sale,
  SettlementStatus,
  SaleDetail,
  Supplier,
  TransactionTotals,
  TransactionType,
  MovementType,
} from './types'

/** Paginação: page começa em 0. */
export type PageParams = { page?: number; size?: number }

export const authApi = {
  register: (data: { name: string; email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),
  login: (data: { email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  verifyEmail: (token: string) => apiClient.post<void>('/auth/verify-email', { token }).then((r) => r.data),
  forgotPassword: (email: string) =>
    apiClient.post<{ message: string }>('/auth/forgot-password', { email }).then((r) => r.data),
  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<void>('/auth/reset-password', { token, newPassword }).then((r) => r.data),
}

export const meApi = {
  get: () => apiClient.get<MeInfo>('/me').then((r) => r.data),
  resendVerification: () => apiClient.post<void>('/me/resend-verification').then((r) => r.data),
}

export const adminApi = {
  overview: () => apiClient.get<AdminOverview>('/admin/overview').then((r) => r.data),
}

export const propertiesApi = {
  listMine: () => apiClient.get<PropertySummary[]>('/properties/me').then((r) => r.data),
  create: (data: { organizationId: number; name: string; location?: string; totalAreaHa?: number }) =>
    apiClient.post<PropertySummary>('/properties', data).then((r) => r.data),
}

export const membersApi = {
  list: () => apiClient.get<PropertyMemberInfo[]>('/properties/current/members').then((r) => r.data),
  add: (data: { email: string; role: PropertyRole }) =>
    apiClient.post<PropertyMemberInfo>('/properties/current/members', data).then((r) => r.data),
  updateRole: (id: number, role: PropertyRole) =>
    apiClient.put<PropertyMemberInfo>(`/properties/current/members/${id}`, { role }).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/properties/current/members/${id}`).then((r) => r.data),
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
  update: (id: number, data: ActivityInput) =>
    apiClient.put<Activity>(`/activities/${id}`, data).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/activities/${id}`).then((r) => r.data),
}

type PartyInput = { name: string; document?: string; phone?: string; email?: string }

type TransactionInput = {
  type: TransactionType
  description: string
  amount: number
  transactionDate: string
  costCenterId?: number
}

type PayableInput = { description: string; amount: number; dueDate: string; supplierId?: number }
type ReceivableInput = { description: string; amount: number; dueDate: string; customerId?: number }
type CostCenterInput = { name: string; activityId?: number }

export const financeApi = {
  cashFlow: (from?: string, to?: string) =>
    apiClient.get<CashFlow>('/finance/cash-flow', { params: { from, to } }).then((r) => r.data),

  listTransactions: (params: PageParams & { from?: string; to?: string; type?: TransactionType; q?: string }) =>
    apiClient.get<PageResponse<FinancialTransaction>>('/finance/transactions', { params }).then((r) => r.data),
  transactionTotals: (from?: string, to?: string) =>
    apiClient.get<TransactionTotals>('/finance/transactions/totals', { params: { from, to } }).then((r) => r.data),
  createTransaction: (data: TransactionInput) =>
    apiClient.post<FinancialTransaction>('/finance/transactions', data).then((r) => r.data),
  updateTransaction: (id: number, data: TransactionInput) =>
    apiClient.put<FinancialTransaction>(`/finance/transactions/${id}`, data).then((r) => r.data),
  removeTransaction: (id: number) => apiClient.delete<void>(`/finance/transactions/${id}`).then((r) => r.data),

  listPayables: (params: PageParams & { status?: SettlementStatus; q?: string }) =>
    apiClient.get<PageResponse<AccountPayable>>('/finance/payables', { params }).then((r) => r.data),
  createPayable: (data: PayableInput) => apiClient.post<AccountPayable>('/finance/payables', data).then((r) => r.data),
  updatePayable: (id: number, data: PayableInput) =>
    apiClient.put<AccountPayable>(`/finance/payables/${id}`, data).then((r) => r.data),
  removePayable: (id: number) => apiClient.delete<void>(`/finance/payables/${id}`).then((r) => r.data),
  payPayable: (id: number) => apiClient.patch<AccountPayable>(`/finance/payables/${id}/pay`).then((r) => r.data),
  unpayPayable: (id: number) => apiClient.patch<AccountPayable>(`/finance/payables/${id}/unpay`).then((r) => r.data),

  listReceivables: (params: PageParams & { status?: SettlementStatus; q?: string }) =>
    apiClient.get<PageResponse<AccountReceivable>>('/finance/receivables', { params }).then((r) => r.data),
  createReceivable: (data: ReceivableInput) =>
    apiClient.post<AccountReceivable>('/finance/receivables', data).then((r) => r.data),
  updateReceivable: (id: number, data: ReceivableInput) =>
    apiClient.put<AccountReceivable>(`/finance/receivables/${id}`, data).then((r) => r.data),
  removeReceivable: (id: number) => apiClient.delete<void>(`/finance/receivables/${id}`).then((r) => r.data),
  receiveReceivable: (id: number) =>
    apiClient.patch<AccountReceivable>(`/finance/receivables/${id}/receive`).then((r) => r.data),
  unreceiveReceivable: (id: number) =>
    apiClient.patch<AccountReceivable>(`/finance/receivables/${id}/unreceive`).then((r) => r.data),

  listCostCenters: () => apiClient.get<CostCenter[]>('/finance/cost-centers').then((r) => r.data),
  createCostCenter: (data: CostCenterInput) =>
    apiClient.post<CostCenter>('/finance/cost-centers', data).then((r) => r.data),
  updateCostCenter: (id: number, data: CostCenterInput) =>
    apiClient.put<CostCenter>(`/finance/cost-centers/${id}`, data).then((r) => r.data),
  removeCostCenter: (id: number) => apiClient.delete<void>(`/finance/cost-centers/${id}`).then((r) => r.data),
}

type ProductInput = { name: string; category?: string; unit: string; minStock: number }

export const inventoryApi = {
  listProducts: (includeArchived = false) =>
    apiClient.get<Product[]>('/inventory/products', { params: { includeArchived } }).then((r) => r.data),
  createProduct: (data: ProductInput) => apiClient.post<Product>('/inventory/products', data).then((r) => r.data),
  updateProduct: (id: number, data: ProductInput) =>
    apiClient.put<Product>(`/inventory/products/${id}`, data).then((r) => r.data),
  removeProduct: (id: number) => apiClient.delete<void>(`/inventory/products/${id}`).then((r) => r.data),
  archiveProduct: (id: number) => apiClient.post<Product>(`/inventory/products/${id}/archive`).then((r) => r.data),
  unarchiveProduct: (id: number) => apiClient.post<Product>(`/inventory/products/${id}/unarchive`).then((r) => r.data),
  lowStock: () => apiClient.get<Product[]>('/inventory/products/low-stock').then((r) => r.data),
  listMovements: (params: PageParams & { productId?: number; from?: string; to?: string }) =>
    apiClient.get<PageResponse<InventoryMovement>>('/inventory/movements', { params }).then((r) => r.data),
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
  list: (params: PageParams & { supplierId?: number; from?: string; to?: string } = {}) =>
    apiClient.get<PageResponse<Purchase>>('/purchases', { params }).then((r) => r.data),
  create: (data: {
    supplierId: number
    purchaseDate: string
    items: { productId: number; quantity: number; unitPrice: number }[]
    paymentType?: PaymentType
    installments?: InstallmentInput[]
  }) => apiClient.post<Purchase>('/purchases', data).then((r) => r.data),
  cancel: (id: number) => apiClient.post<Purchase>(`/purchases/${id}/cancel`).then((r) => r.data),
  detail: (id: number) => apiClient.get<PurchaseDetail>(`/purchases/${id}`).then((r) => r.data),
  listSuppliers: () => apiClient.get<Supplier[]>('/purchases/suppliers').then((r) => r.data),
  createSupplier: (data: PartyInput) => apiClient.post<Supplier>('/purchases/suppliers', data).then((r) => r.data),
  updateSupplier: (id: number, data: PartyInput) =>
    apiClient.put<Supplier>(`/purchases/suppliers/${id}`, data).then((r) => r.data),
  removeSupplier: (id: number) => apiClient.delete<void>(`/purchases/suppliers/${id}`).then((r) => r.data),
}

export const salesApi = {
  list: (params: PageParams & { customerId?: number; from?: string; to?: string } = {}) =>
    apiClient.get<PageResponse<Sale>>('/sales', { params }).then((r) => r.data),
  create: (data: {
    customerId: number
    saleDate: string
    items: { productId: number; quantity: number; unitPrice: number }[]
    paymentType?: PaymentType
    installments?: InstallmentInput[]
  }) => apiClient.post<Sale>('/sales', data).then((r) => r.data),
  cancel: (id: number) => apiClient.post<Sale>(`/sales/${id}/cancel`).then((r) => r.data),
  detail: (id: number) => apiClient.get<SaleDetail>(`/sales/${id}`).then((r) => r.data),
  listCustomers: () => apiClient.get<Customer[]>('/sales/customers').then((r) => r.data),
  createCustomer: (data: PartyInput) => apiClient.post<Customer>('/sales/customers', data).then((r) => r.data),
  updateCustomer: (id: number, data: PartyInput) =>
    apiClient.put<Customer>(`/sales/customers/${id}`, data).then((r) => r.data),
  removeCustomer: (id: number) => apiClient.delete<void>(`/sales/customers/${id}`).then((r) => r.data),
}

type ProductionInput = {
  productName: string
  quantity: number
  unit: string
  productionDate: string
  activityId?: number
}

export const productionApi = {
  list: (params: PageParams & { from?: string; to?: string; q?: string } = {}) =>
    apiClient.get<PageResponse<ProductionRecord>>('/production', { params }).then((r) => r.data),
  create: (data: ProductionInput) => apiClient.post<ProductionRecord>('/production', data).then((r) => r.data),
  update: (id: number, data: ProductionInput) =>
    apiClient.put<ProductionRecord>(`/production/${id}`, data).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/production/${id}`).then((r) => r.data),
}

export const dashboardApi = {
  summary: (from?: string, to?: string) =>
    apiClient.get<DashboardSummary>('/dashboard/summary', { params: { from, to } }).then((r) => r.data),
}

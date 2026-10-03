import { apiClient } from './client'
import type {
  Activity,
  ActivityInput,
  AccountPayable,
  AccountReceivable,
  AdminOverview,
  Animal,
  AnimalBatch,
  AnimalDetail,
  AnimalInput,
  AnimalStatus,
  AuditAuthor,
  BatchInput,
  Crop,
  CropCycle,
  CropCycleInput,
  CropInput,
  CropHarvest,
  CropHarvestInput,
  CropIndicators,
  CropOperation,
  CropOperationInput,
  ExitType,
  Field,
  FieldInput,
  HealthApplication,
  HealthApplicationInput,
  HerdSummary,
  LivestockAlerts,
  AnimalEvent,
  LivestockTrade,
  LivestockIndicators,
  LivestockTradeDetail,
  NewAnimalInput,
  ReproductiveEvent,
  ReproductiveEventInput,
  SpeciesCatalogItem,
  SpeciesInput,
  TradeKind,
  AuditEntry,
  AuthResponse,
  DeletionPreview,
  InvitationInfo,
  InvitationPreview,
  InviteResult,
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
  register: (data: { name: string; email: string; password: string; termsVersion: string }) =>
    apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),
  login: (data: { email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  verifyEmail: (token: string) => apiClient.post<void>('/auth/verify-email', { token }).then((r) => r.data),
  forgotPassword: (email: string) =>
    apiClient.post<{ message: string }>('/auth/forgot-password', { email }).then((r) => r.data),
  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<void>('/auth/reset-password', { token, newPassword }).then((r) => r.data),
  confirmEmailChange: (token: string) =>
    apiClient.post<void>('/auth/confirm-email-change', { token }).then((r) => r.data),
}

export const meApi = {
  get: () => apiClient.get<MeInfo>('/me').then((r) => r.data),
  resendVerification: () => apiClient.post<void>('/me/resend-verification').then((r) => r.data),
  updateName: (name: string) => apiClient.put<MeInfo>('/me', { name }).then((r) => r.data),
  /** Devolve um token novo: as outras sessões são encerradas. */
  changePassword: (currentPassword: string, newPassword: string) =>
    apiClient.post<AuthResponse>('/me/password', { currentPassword, newPassword }).then((r) => r.data),
  requestEmailChange: (newEmail: string, currentPassword: string) =>
    apiClient.post<void>('/me/email', { newEmail, currentPassword }).then((r) => r.data),
  cancelEmailChange: () => apiClient.delete<void>('/me/email').then((r) => r.data),
  acceptTerms: (termsVersion: string) => apiClient.post<MeInfo>('/me/accept-terms', { termsVersion }).then((r) => r.data),
  /** Arquivo JSON com todos os dados da pessoa (LGPD, portabilidade). */
  exportData: () => apiClient.get<Blob>('/me/export', { responseType: 'blob' }).then((r) => r.data),
  deletionPreview: () => apiClient.get<DeletionPreview>('/me/deletion-preview').then((r) => r.data),
  deleteAccount: (password: string) => apiClient.delete<void>('/me', { data: { password } }).then((r) => r.data),
}

export const adminApi = {
  overview: () => apiClient.get<AdminOverview>('/admin/overview').then((r) => r.data),
}

export const propertiesApi = {
  listMine: () => apiClient.get<PropertySummary[]>('/properties/me').then((r) => r.data),
  create: (data: { organizationId: number; name: string; location?: string; totalAreaHa?: number }) =>
    apiClient.post<PropertySummary>('/properties', data).then((r) => r.data),
  updateCurrent: (data: { name: string; location: string | null; totalAreaHa: number | null }) =>
    apiClient.put<PropertySummary>('/properties/current', data).then((r) => r.data),
  /** Definitivo: apaga a propriedade e todos os dados dela. */
  deleteCurrent: (confirmName: string, password: string) =>
    apiClient.delete<void>('/properties/current', { data: { confirmName, password } }).then((r) => r.data),
}

export const invitationsApi = {
  invite: (data: { email: string; role: PropertyRole }) =>
    apiClient.post<InviteResult>('/properties/current/invitations', data).then((r) => r.data),
  list: () => apiClient.get<InvitationInfo[]>('/properties/current/invitations').then((r) => r.data),
  resend: (id: number) =>
    apiClient.post<InvitationInfo>(`/properties/current/invitations/${id}/resend`).then((r) => r.data),
  cancel: (id: number) => apiClient.delete<void>(`/properties/current/invitations/${id}`).then((r) => r.data),
  preview: (token: string) =>
    apiClient.post<InvitationPreview>('/auth/invitations/preview', { token }).then((r) => r.data),
  register: (token: string, name: string, password: string, termsVersion: string) =>
    apiClient
      .post<AuthResponse>('/auth/invitations/register', { token, name, password, termsVersion })
      .then((r) => r.data),
  accept: (token: string) => apiClient.post<PropertySummary>('/invitations/accept', { token }).then((r) => r.data),
}

export const auditApi = {
  search: (params: { from: string; to: string; userId?: number; entity?: string[] } & PageParams) =>
    apiClient
      .get<PageResponse<AuditEntry>>('/properties/current/audit', {
        params,
        // entity=A&entity=B (sem colchetes), como o Spring espera.
        paramsSerializer: { indexes: null },
      })
      .then((r) => r.data),
  authors: () => apiClient.get<AuditAuthor[]>('/properties/current/audit/authors').then((r) => r.data),
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

export const fieldsApi = {
  list: (includeArchived = false) => apiClient.get<Field[]>('/fields', { params: { includeArchived } }).then((r) => r.data),
  create: (data: FieldInput) => apiClient.post<Field>('/fields', data).then((r) => r.data),
  update: (id: number, data: FieldInput) => apiClient.put<Field>(`/fields/${id}`, data).then((r) => r.data),
  archive: (id: number) => apiClient.post<Field>(`/fields/${id}/archive`).then((r) => r.data),
  unarchive: (id: number) => apiClient.post<Field>(`/fields/${id}/unarchive`).then((r) => r.data),
}

export const cropsApi = {
  list: (includeArchived = false) => apiClient.get<Crop[]>('/crops', { params: { includeArchived } }).then((r) => r.data),
  create: (data: CropInput) => apiClient.post<Crop>('/crops', data).then((r) => r.data),
  archive: (id: number) => apiClient.post<Crop>(`/crops/${id}/archive`).then((r) => r.data),
  unarchive: (id: number) => apiClient.post<Crop>(`/crops/${id}/unarchive`).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/crops/${id}`).then((r) => r.data),
}

export const cropCyclesApi = {
  list: (includeArchived = false) =>
    apiClient.get<CropCycle[]>('/crop-cycles', { params: { includeArchived } }).then((r) => r.data),
  create: (data: CropCycleInput) => apiClient.post<CropCycle>('/crop-cycles', data).then((r) => r.data),
  archive: (id: number) => apiClient.post<CropCycle>(`/crop-cycles/${id}/archive`).then((r) => r.data),
  unarchive: (id: number) => apiClient.post<CropCycle>(`/crop-cycles/${id}/unarchive`).then((r) => r.data),
}

export const cropOperationsApi = {
  list: (params: PageParams & { cropCycleId?: number; fieldId?: number; from?: string; to?: string } = {}) =>
    apiClient.get<PageResponse<CropOperation>>('/crop-operations', { params }).then((r) => r.data),
  create: (data: CropOperationInput) => apiClient.post<CropOperation>('/crop-operations', data).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/crop-operations/${id}`).then((r) => r.data),
}

export const cropHarvestsApi = {
  list: (params: PageParams & { cropCycleId?: number; fieldId?: number; from?: string; to?: string } = {}) =>
    apiClient.get<PageResponse<CropHarvest>>('/crop-harvests', { params }).then((r) => r.data),
  create: (data: CropHarvestInput) => apiClient.post<CropHarvest>('/crop-harvests', data).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/crop-harvests/${id}`).then((r) => r.data),
}

export const cropIndicatorsApi = {
  get: (params: { cropCycleId?: number; fieldId?: number; from?: string; to?: string } = {}) =>
    apiClient.get<CropIndicators>('/crop-indicators', { params }).then((r) => r.data),
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

export const speciesApi = {
  list: (includeArchived = false) =>
    apiClient.get<SpeciesCatalogItem[]>('/livestock/species', { params: { includeArchived } }).then((r) => r.data),
  create: (data: SpeciesInput) => apiClient.post<SpeciesCatalogItem>('/livestock/species', data).then((r) => r.data),
  archive: (id: number) => apiClient.post<SpeciesCatalogItem>(`/livestock/species/${id}/archive`).then((r) => r.data),
  unarchive: (id: number) => apiClient.post<SpeciesCatalogItem>(`/livestock/species/${id}/unarchive`).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/livestock/species/${id}`).then((r) => r.data),
}

export const livestockApi = {
  summary: () => apiClient.get<HerdSummary>('/livestock/summary').then((r) => r.data),

  listBatches: (includeArchived = false) =>
    apiClient.get<AnimalBatch[]>('/livestock/batches', { params: { includeArchived } }).then((r) => r.data),
  createBatch: (data: BatchInput) => apiClient.post<AnimalBatch>('/livestock/batches', data).then((r) => r.data),
  updateBatch: (id: number, data: BatchInput) =>
    apiClient.put<AnimalBatch>(`/livestock/batches/${id}`, data).then((r) => r.data),
  archiveBatch: (id: number) => apiClient.post<AnimalBatch>(`/livestock/batches/${id}/archive`).then((r) => r.data),
  unarchiveBatch: (id: number) => apiClient.post<AnimalBatch>(`/livestock/batches/${id}/unarchive`).then((r) => r.data),
  deleteBatch: (id: number) => apiClient.delete<void>(`/livestock/batches/${id}`).then((r) => r.data),
  addHeads: (id: number, data: { headCount: number; date: string; description?: string }) =>
    apiClient.post<AnimalBatch>(`/livestock/batches/${id}/entries`, data).then((r) => r.data),
  removeHeads: (id: number, data: { headCount: number; date: string; exitType: ExitType; description?: string }) =>
    apiClient.post<AnimalBatch>(`/livestock/batches/${id}/exits`, data).then((r) => r.data),

  listAnimals: (params: { status?: AnimalStatus; batchId?: number; speciesId?: number; q?: string } & PageParams) =>
    apiClient.get<PageResponse<Animal>>('/livestock/animals', { params }).then((r) => r.data),
  animal: (id: number) => apiClient.get<AnimalDetail>(`/livestock/animals/${id}`).then((r) => r.data),
  createAnimal: (data: AnimalInput) => apiClient.post<Animal>('/livestock/animals', data).then((r) => r.data),
  updateAnimal: (id: number, data: AnimalInput) =>
    apiClient.put<Animal>(`/livestock/animals/${id}`, data).then((r) => r.data),
  deleteAnimal: (id: number) => apiClient.delete<void>(`/livestock/animals/${id}`).then((r) => r.data),
  moveAnimals: (data: { animalIds: number[]; toBatchId: number | null; date: string }) =>
    apiClient.post<Animal[]>('/livestock/animals/move', data).then((r) => r.data),
  exitAnimal: (id: number, data: { type: ExitType; date: string; reason?: string }) =>
    apiClient.post<Animal>(`/livestock/animals/${id}/exit`, data).then((r) => r.data),
  reactivateAnimal: (id: number) => apiClient.post<Animal>(`/livestock/animals/${id}/reactivate`).then((r) => r.data),

  listTrades: (params: { kind?: TradeKind } & PageParams) =>
    apiClient.get<PageResponse<LivestockTrade>>('/livestock/trades', { params }).then((r) => r.data),
  trade: (id: number) => apiClient.get<LivestockTradeDetail>(`/livestock/trades/${id}`).then((r) => r.data),
  purchase: (data: {
    supplierId: number
    tradeDate: string
    totalAmount: number
    paymentType: PaymentType
    installments?: InstallmentInput[]
    batchId?: number | null
    speciesId?: number
    animals?: NewAnimalInput[]
    headCount?: number
    notes?: string
  }) => apiClient.post<LivestockTrade>('/livestock/purchases', data).then((r) => r.data),
  sale: (data: {
    customerId: number
    tradeDate: string
    totalAmount: number
    paymentType: PaymentType
    installments?: InstallmentInput[]
    animalIds?: number[]
    batchId?: number | null
    headCount?: number
    notes?: string
  }) => apiClient.post<LivestockTrade>('/livestock/sales', data).then((r) => r.data),
  cancelTrade: (id: number) => apiClient.post<LivestockTrade>(`/livestock/trades/${id}/cancel`).then((r) => r.data),

  weigh: (data: {
    date: string
    entries?: { animalId: number; weight: number }[]
    batchId?: number
    averageWeight?: number
    sampleSize?: number
  }) => apiClient.post<AnimalEvent[]>('/livestock/weighings', data).then((r) => r.data),
  deleteWeighing: (eventId: number) => apiClient.delete<void>(`/livestock/weighings/${eventId}`).then((r) => r.data),

  listHealth: (params: PageParams) =>
    apiClient.get<PageResponse<HealthApplication>>('/livestock/health', { params }).then((r) => r.data),
  applyHealth: (data: HealthApplicationInput) =>
    apiClient.post<HealthApplication>('/livestock/health', data).then((r) => r.data),
  undoHealth: (id: number) => apiClient.delete<void>(`/livestock/health/${id}`).then((r) => r.data),
  dismissFollowUp: (id: number) =>
    apiClient.post<HealthApplication>(`/livestock/health/${id}/follow-up-done`).then((r) => r.data),
  alerts: () => apiClient.get<LivestockAlerts>('/livestock/alerts').then((r) => r.data),
}

export const reproductionApi = {
  list: (params: { animalId?: number } & PageParams) =>
    apiClient.get<PageResponse<ReproductiveEvent>>('/livestock/reproduction', { params }).then((r) => r.data),
  register: (data: ReproductiveEventInput) =>
    apiClient.post<ReproductiveEvent>('/livestock/reproduction', data).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/livestock/reproduction/${id}`).then((r) => r.data),
}

export const indicatorsApi = {
  get: (params: { batchId?: number; from?: string; to?: string }) =>
    apiClient.get<LivestockIndicators>('/livestock/indicators', { params }).then((r) => r.data),
}

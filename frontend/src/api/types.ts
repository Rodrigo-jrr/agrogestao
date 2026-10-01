export type PropertyRole = 'OWNER' | 'MANAGER' | 'WORKER' | 'VIEWER'

export interface AuthResponse {
  token: string
  userId: number
  name: string
  email: string
}

export interface PropertySummary {
  id: number
  organizationId: number
  name: string
  location: string | null
  totalAreaHa: number | null
  role: PropertyRole
}

export interface ActivityInput {
  name: string
  type: string
  areaHa?: number | null
}

export interface Activity {
  id: number
  name: string
  type: string
  areaHa: number | null
  responsible: string | null
}

export type TransactionType = 'INCOME' | 'EXPENSE'

export interface FinancialTransaction {
  id: number
  type: TransactionType
  description: string
  amount: number
  transactionDate: string
  costCenterId: number | null
}

export interface AccountPayable {
  id: number
  description: string
  amount: number
  dueDate: string
  supplierId: number | null
  paid: boolean
  purchaseId: number | null
  /** Parcela de compra de animais (pecuária). */
  livestockTradeId: number | null
  installmentNumber: number | null
  installmentCount: number | null
}

export interface AccountReceivable {
  id: number
  description: string
  amount: number
  dueDate: string
  customerId: number | null
  received: boolean
  saleId: number | null
  /** Parcela de venda de animais (pecuária). */
  livestockTradeId: number | null
  installmentNumber: number | null
  installmentCount: number | null
}

export interface CostCenter {
  id: number
  name: string
  activityId: number | null
}

export interface Product {
  id: number
  name: string
  category: string | null
  unit: string
  minStock: number
  currentQuantity: number
  averageCost: number
  stockValue: number
  archived: boolean
}

export type MovementType = 'IN' | 'OUT' | 'ADJUST'

export interface InventoryMovement {
  id: number
  productId: number
  productName: string | null
  unit: string | null
  type: MovementType
  quantity: number
  unitCost: number | null
  movementDate: string
  notes: string | null
}

export interface Supplier {
  id: number
  name: string
  document: string | null
  phone: string | null
  email: string | null
}

export interface Customer {
  id: number
  name: string
  document: string | null
  phone: string | null
  email: string | null
}

export type PaymentType = 'CASH' | 'INSTALLMENTS'

export interface InstallmentInput {
  dueDate: string
  amount: number
}

export interface InstallmentView {
  accountId: number
  number: number
  count: number
  dueDate: string
  amount: number
  settled: boolean
}

export interface Purchase {
  id: number
  supplierId: number
  purchaseDate: string
  totalAmount: number
  canceled: boolean
  paymentType: PaymentType
  installmentCount: number
}

export interface TradeDetailItem {
  productId: number
  productName: string | null
  unit: string | null
  quantity: number
  unitPrice: number
  unitCost?: number | null
  total: number
}

export interface PurchaseDetail {
  id: number
  supplierId: number
  supplierName: string | null
  purchaseDate: string
  totalAmount: number
  paymentType: PaymentType
  canceled: boolean
  items: TradeDetailItem[]
  installments: InstallmentView[]
}

export interface SaleDetail {
  id: number
  customerId: number
  customerName: string | null
  saleDate: string
  totalAmount: number
  totalCost: number | null
  margin: number | null
  paymentType: PaymentType
  canceled: boolean
  items: TradeDetailItem[]
  installments: InstallmentView[]
}

export interface Sale {
  id: number
  customerId: number
  saleDate: string
  totalAmount: number
  canceled: boolean
  paymentType: PaymentType
  installmentCount: number
}

export interface ProductionRecord {
  id: number
  productName: string
  quantity: number
  unit: string
  productionDate: string
  activityId: number | null
}

export interface ProductionSummaryItem {
  productName: string
  totalQuantity: number
  unit: string
}

export interface DashboardSummary {
  periodFrom: string
  periodTo: string
  totalIncome: number
  totalExpense: number
  result: number
  production: ProductionSummaryItem[]
  lowStockAlerts: Product[]
}

export interface PropertyMemberInfo {
  id: number
  userId: number
  name: string
  email: string | null
  role: PropertyRole
  currentUser: boolean
}

export interface CashFlowMonth {
  month: string
  income: number
  expense: number
  net: number
  balance: number
  pendingReceivables: number
  pendingPayables: number
  projectedBalance: number
}

export interface CashFlow {
  from: string
  to: string
  openingBalance: number
  overdueReceivables: number
  overduePayables: number
  months: CashFlowMonth[]
}

export interface MeInfo {
  userId: number
  name: string
  email: string
  emailVerified: boolean
  platformAdmin: boolean
  /** E-mail novo aguardando confirmação pelo link enviado a ele. */
  pendingEmail: string | null
  /** Aceitou a versão atual dos termos de uso e da política de privacidade. */
  termsAccepted: boolean
  termsVersion: string
}

export interface AdminPropertyRow {
  id: number
  name: string
  location: string | null
  totalAreaHa: number | null
  members: number
  createdAt: string
  lastActivityAt: string | null
}

export interface AdminOrganizationRow {
  id: number
  name: string
  ownerName: string | null
  ownerEmail: string | null
  createdAt: string
  lastActivityAt: string | null
  properties: AdminPropertyRow[]
}

export interface AdminUserRow {
  id: number
  name: string
  email: string
  properties: number
  platformAdmin: boolean
  createdAt: string
  lastActivityAt: string | null
}

export interface AdminOverview {
  summary: {
    organizations: number
    properties: number
    users: number
    newUsersLast30Days: number
    activePropertiesLast30Days: number
  }
  organizations: AdminOrganizationRow[]
  users: AdminUserRow[]
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TransactionTotals {
  income: number
  expense: number
  result: number
}

export type SettlementStatus = 'OPEN' | 'SETTLED' | 'ALL'

export interface InvitationInfo {
  id: number
  email: string
  role: PropertyRole
  invitedByName: string | null
  createdAt: string
  expiresAt: string
  expired: boolean
}

/** ADDED: já tinha conta e entrou direto. INVITED: recebeu convite por e-mail. */
export interface InviteResult {
  outcome: 'ADDED' | 'INVITED'
  member: PropertyMemberInfo | null
  invitation: InvitationInfo | null
}

export interface InvitationPreview {
  propertyName: string
  invitedByName: string | null
  email: string
  role: PropertyRole
  accountExists: boolean
}

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'CANCEL' | 'VIEW'

export interface AuditEntry {
  id: number
  createdAt: string
  userId: number | null
  userName: string | null
  entityName: string
  entityId: number
  action: AuditAction
  details: string | null
}

export interface AuditAuthor {
  userId: number
  name: string
}

export interface PropertyRef {
  id: number
  name: string
}

/** O que acontece com cada propriedade ao excluir a conta. */
export interface DeletionPreview {
  toDelete: PropertyRef[]
  toLeave: PropertyRef[]
  blocking: PropertyRef[]
}

// Pecuária

export type Species = 'BOVINO' | 'BUBALINO' | 'OVINO' | 'CAPRINO' | 'SUINO' | 'EQUINO' | 'AVE' | 'PEIXE' | 'OUTRO'
export type Sex = 'MACHO' | 'FEMEA'
export type AnimalOrigin = 'NASCIMENTO' | 'COMPRA' | 'OUTRA'
export type AnimalStatus = 'ATIVO' | 'VENDIDO' | 'MORTO' | 'DESCARTADO'
export type BatchTracking = 'INDIVIDUAL' | 'AGGREGATE'
export type ExitType = 'MORTE' | 'DESCARTE' | 'OUTRA'
export type AnimalEventType = 'ENTRADA' | 'MUDANCA_LOTE' | 'SAIDA'
export type TradeKind = 'PURCHASE' | 'SALE'

export interface AnimalBatch {
  id: number
  name: string
  species: Species
  tracking: BatchTracking
  headCount: number
  activityId: number | null
  activityName: string | null
  notes: string | null
  archived: boolean
}

export interface BatchInput {
  name: string
  species: Species
  tracking: BatchTracking
  activityId: number | null
  notes: string | null
}

export interface Animal {
  id: number
  tag: string
  name: string | null
  species: Species
  breed: string | null
  sex: Sex
  birthDate: string | null
  birthDateEstimated: boolean
  origin: AnimalOrigin
  entryDate: string
  motherId: number | null
  motherTag: string | null
  sire: string | null
  batchId: number | null
  batchName: string | null
  status: AnimalStatus
  exitDate: string | null
  exitReason: string | null
  currentWeight: number | null
  notes: string | null
}

export interface AnimalInput {
  tag: string
  name: string | null
  species: Species
  breed: string | null
  sex: Sex
  birthDate: string | null
  birthDateEstimated: boolean
  origin?: AnimalOrigin
  entryDate?: string | null
  motherId: number | null
  sire: string | null
  batchId?: number | null
  weight?: number | null
  notes: string | null
}

export interface AnimalEvent {
  id: number
  type: AnimalEventType
  date: string
  animalId: number | null
  animalTag: string | null
  batchId: number | null
  batchName: string | null
  headCount: number | null
  weight: number | null
  fromBatchName: string | null
  toBatchName: string | null
  tradeId: number | null
  amount: number | null
  description: string | null
}

export interface AnimalDetail {
  animal: Animal
  events: AnimalEvent[]
  offspring: Animal[]
}

export interface NewAnimalInput {
  tag: string
  name?: string | null
  sex: Sex
  breed?: string | null
  birthDate?: string | null
  weight?: number | null
}

export interface LivestockTrade {
  id: number
  kind: TradeKind
  partyId: number
  partyName: string
  tradeDate: string
  headCount: number
  totalAmount: number
  paymentType: PaymentType
  installmentCount: number
  batchId: number | null
  batchName: string | null
  notes: string | null
  canceled: boolean
}

export interface LivestockTradeDetail {
  trade: LivestockTrade
  animals: Animal[]
  installments: InstallmentView[]
}

export interface HerdSummary {
  totalHeads: number
  bySpecies: { species: Species; heads: number }[]
}

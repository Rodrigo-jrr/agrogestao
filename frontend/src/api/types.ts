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
